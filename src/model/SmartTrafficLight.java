package model;

import application.Application;
import application.trafficLight.ApplicationTrafficLightControl;
import application.trafficLight.ApplicationTrafficLightControlData;
import controller.newengine.SimulationEngine;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import model.parameters.Globals;
import utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SmartTrafficLight extends TrafficLightModel {

    private long sumWaitingTime = 0;
    private long sumQueueLength = 0;
    private long noWaits = 0;

    private final Integer lockQueue = 1;
    private boolean complementarySwitching = true;

    // tells what group is green in a moment of time
    private int greenGroup;
    private Pair<List<TrafficLightView>, List<TrafficLightView>> complementaryGroups;
    private List<Integer> carsRemainedInQueue;

    // 1.If there are many long queues, then maxTime progressively increase his value to Globals.maxTrafficLightTime
    // 2.Covers the cases when there are big queues just for one direction, and from the other there are few;
    // It is FAIR for 2,3 cars to not wait for 15 to pass
    private long  inferiorLimitMaxTime = 60;
    private long maxTime = inferiorLimitMaxTime;
    private final long normalTime = Globals.normalTrafficLightTime;
    private final int maximumCheckPhases = 4;
    private int checkPhase = 1;

    private long lastTimeUpdate;
    private long decidedTime;
    private boolean shouldChangeColor;

    private boolean receivedEmergencySignal = false;
    private long emergencyTime;
    private boolean isEmergencyMode = false;
    private TrafficLightView tlvEmergency = null;

    private void initComplementaryPairs() {
        greenGroup = 0;
        complementaryGroups = new Pair(new ArrayList<>(), new ArrayList<>());
        carsRemainedInQueue =  new ArrayList<>(Collections.nCopies(2, 0));
    }

    public SmartTrafficLight(long id, Node node) {
        super(id, node);
        this.lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
        this.decidedTime = this.normalTime;
        this.shouldChangeColor = false;
        initComplementaryPairs();
    }

    public Node getLocation() {
        return this.emplacement;
    }

    public void addTrafficLightView(TrafficLightView trafficLight) {
        this.trafficLightViewList.add(trafficLight);

        if(complementarySwitching) {
            if (trafficLight.getColorString().equals("green")) {
                complementaryGroups.getFirst().add(trafficLight);
            } else if (trafficLight.getColorString().equals("red")){
                complementaryGroups.getSecond().add(trafficLight);
            }
        }
    }

    public List<TrafficLightView> getLightsByColor(String color) {
        if (greenGroup == 0 && color.equals("green")) return complementaryGroups.getFirst();
        if (greenGroup == 0 && color.equals("red")) return complementaryGroups.getSecond();
        if (greenGroup == 1 && color.equals("red")) return complementaryGroups.getFirst();
        if (greenGroup == 1 && color.equals("green")) return complementaryGroups.getSecond();
        return null;
    }


    public boolean canCommute() {
        return SimulationEngine.getInstance().getSimulationTime() - this.lastTimeUpdate > this.decidedTime;
    }

    public int sumCarsRemainedInQueues() {
        return carsRemainedInQueue.stream().mapToInt(noCars -> noCars).sum();
    }

    public long computeTimeRaise() {
        return (long) sumCarsRemainedInQueues() * Globals.passIntersectionTime / trafficLightViewList.size() ;
    }

    public void changeColor() {

        if (SimulationEngine.getInstance().getSimulationTime() < 250) {
            return;
        }

        // reset after 1 hour
        if (SimulationEngine.getInstance().getSimulationTime() % 3600 == 0) {
            this.maxTime = inferiorLimitMaxTime;
        }
        // if enters this block, it should commute, meaning take a decision based on QueuesLen
        if (canCommute()) {

            // after the last big queue that passed, switching time between lights should be the minimum one or
            // the normal one; cannot risk to give green light to nobody,then 5 cars are going to the red tf
            // and wait for nothing
            if (waitingQueue.size() == 0 && decidedTime > normalTime) {
                if (carsRemainedInQueue.get(getRedGroupId()) == 0) {
                    decidedTime = Globals.passIntersectionTime;
                } else {
                    decidedTime = normalTime;
                }
            }

            // minimum decided time is time to pass for a single car; we can check by maximumCheckPhases=4 times to see
            // if there are incoming cars and, if there are not, we should reset to normal.
            if (waitingQueue.size() == 0 && decidedTime < normalTime) {
                if (checkPhase == maximumCheckPhases) {
                    decidedTime = normalTime;
                    checkPhase = 1;
                } else {
                    checkPhase++;
                }
            } else {
                // if any car came to a traffic light, reset checkPhase
                checkPhase = 1;
            }

            // improve maxTime one time per cycle (green -> red -> green)
            if (greenGroup == 0 && sumCarsRemainedInQueues() > 0) {
                long timeRaise = computeTimeRaise();
                if (this.maxTime + timeRaise < Globals.maxTrafficLightTime) {
                    this.maxTime += timeRaise;
                }
                carsRemainedInQueue.set(0, 0);
                carsRemainedInQueue.set(1, 0);
            }


            // exit emergencyMode by restoring the lights color
            if (isEmergencyMode) {
                tlvEmergency.changeColor();
                // these are the green lights which were switch to red after the emergency signal
                getLightsByColor("green").forEach(greenTLV -> {
                    greenTLV.changeColor();
                });
                isEmergencyMode = false;
            }
            // enters only if there are cars at the red light
            if (Globals.useDynamicTrafficLights && complementarySwitching && waitingQueue.size() > 0) {

                long maxQueueLen = -1;
                Pair<Long, Integer> longQueueStreet = null;
                long allCarsStopped = 0;
                List<Integer> longQueues = new ArrayList<>();
                int queueLenCanPass = (int) (this.maxTime / Globals.passIntersectionTime);

                synchronized (lockQueue) {
                    for (Pair<Long, Integer> key : waitingQueue.keySet()) {
                        int currentQueueLen = waitingQueue.get(key).getSecond();
                        allCarsStopped += currentQueueLen;

                        if (currentQueueLen > maxQueueLen) {
                            maxQueueLen = currentQueueLen;
                            longQueueStreet = key;
                        }

                        if (queueLenCanPass < currentQueueLen) {
                            longQueues.add(currentQueueLen);
                        }
                    }
                }

                if (allCarsStopped > 0) {
                    collectWaitingQueueStatistics(waitingQueue.get(longQueueStreet).getFirst(), waitingQueue.get(longQueueStreet).getSecond());
                    if (longQueues.isEmpty()) {
                        decidedTime = maxQueueLen * Globals.passIntersectionTime;
                        if (maxQueueLen == 1) {
                            decidedTime += Globals.passIntersectionTime;
                        }
                    } else {

                        decidedTime = this.maxTime;
                        int redGroupId = getRedGroupId();

                        longQueues.forEach(queueLength -> {
                            int noCars = carsRemainedInQueue.get(redGroupId);
                            noCars += (queueLength - queueLenCanPass);
                            carsRemainedInQueue.set(redGroupId, noCars);
                        });
                    }
                }
                // SmartTrafficLight already made a decision based on Queue dimension
                waitingQueue.clear();
            }

            this.lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
            switchLightGroups();
            this.shouldChangeColor = true;
        } else if (receivedEmergencySignal) {
            this.lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
            this.decidedTime = emergencyTime;
        } else {
            synchronized (lockQueue) {
                if (waitingQueue.size() == 1 && decidedTime == normalTime) {
                    lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
                    decidedTime = Globals.passIntersectionTime;
                    switchLightGroups();
                    shouldChangeColor = true;
                    collectWaitingQueueStatistics(waitingQueue.get(0).getFirst(), waitingQueue.get(0).getSecond());
                    waitingQueue.clear();
                }
            }
        }

    }

    public void updateTrafficLightViews() {
        if (shouldChangeColor) {
            trafficLightViewList.forEach(tf -> tf.updateTrafficLightView());
            shouldChangeColor = false;
        }

        if (receivedEmergencySignal) {
            getLightsByColor("green").forEach(greenTLV -> {
                greenTLV.updateTrafficLightView();
            });
            tlvEmergency.updateTrafficLightView();

            receivedEmergencySignal = false;
            isEmergencyMode = true;
        }
    }

    /***
     * The master traffic light will put the car that has sent the message to the corresponding waiting queue.
     * @param data
     */
    synchronized public void addCarToQueue(ApplicationTrafficLightControlData data) {

        Pair<Long, Integer> street = new Pair<>(data.getWayId(), data.getDirection());

        if (!waitingQueue.containsKey(street)) {

            // neglect propagation time from sending the message to the time it is received
            Pair<Long, Integer> firstCarInQueue = new Pair<>(data.getTimeStop(), 1);
            waitingQueue.put(street, firstCarInQueue);

        } else {

            // increment queue size and keep waiting time of the first car arrived
            Pair<Long, Integer> updatedQueue =
                    new Pair<>(waitingQueue.get(street).getFirst(), waitingQueue.get(street).getSecond() + 1);
            waitingQueue.put(street, updatedQueue);
        }
    }

    public void setGreenForEmergency(ApplicationTrafficLightControlData data) {

        if (!isEmergencyMode) {
            if (data.getEmergencyTime() != -1) {
                receivedEmergencySignal = true;
                tlvEmergency = findTrafficLightByWay(data.getWayId());
                emergencyTime = data.getEmergencyTime();
            }

        }
    }

    public int getRedGroupId() {
        if (greenGroup == 0)
            return 1;
        return 0;
    }

    public void switchLightGroups() {
        if(greenGroup == 0) {
            greenGroup = 1;
        } else {
            greenGroup = 0;
        }
    }

    public void sendStatistics() {
        if (noWaits == 0)
            return;

        double avg_waitingTime = sumWaitingTime / noWaits;
        double avg_queueLength = sumQueueLength / noWaits;
        ApplicationTrafficLightControl.saveData(this.getId(), avg_waitingTime, avg_queueLength);
    }

    public void collectWaitingQueueStatistics(long stopTime, long noCarsWaiting) {
        sumWaitingTime += (SimulationEngine.getInstance().getSimulationTime() - stopTime);
        sumQueueLength += noCarsWaiting;
        noWaits++;
    }

    public String stopApplications() {
        /* Send statistics */
        sendStatistics();

        String result = "";
        for (Application application : this.applications) {
            result += application.stop();
        }
        return result;
    }

    public String runApplications() {
        String result = "";
        for (Application application : this.applications) {
            result += application.run();
        }
        return result;
    }

}
