package model;

import application.Application;
import application.trafficLight.ApplicationTrafficLightControlData;
import controller.newengine.SimulationEngine;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import model.mobility.MobilityEngine;
import model.parameters.Globals;
import utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SmartTrafficLight extends TrafficLightModel {

    private Integer lockQueue = 1;
    private MobilityEngine mobilityEngine;

    // tells what group is the green in a moment of time
    private boolean complementarySwitching = true;
    private int greenGroup;
    private Pair<List<TrafficLightView>, List<TrafficLightView>> complementaryGroups;
    private List<Integer> carsRemainedInQueue;

    // 1.If there are many long queues, then maxTime progressively increase his value to Globals.maxTrafficLightTime
    // 2.Covers the cases when there are big queues just for one direction, and from the other there are few;
    // It is FAIR for 2,3 cars to not wait for 15 to pass.
    private long maxTime = 60;
    private long normalTime = Globals.normalTrafficLightTime;
    private int checkPhase = 1;
    private int maximumCheckPhases = 4;


    private long lastTimeUpdate;
    private long decidedTime;
    private boolean shouldChangeColor;

    private boolean receivedEmergencySignal = false;
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
        this.mobilityEngine = MobilityEngine.getInstance();
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
        if (SimulationEngine.getInstance().getSimulationTime() - this.lastTimeUpdate > this.decidedTime) {
            return true;
        }
        return false;
    }

    public int sumCarsRemainedInQueues() {
        int total = carsRemainedInQueue.stream().mapToInt(noCars -> noCars).sum();
        return total;
    }

    public long computeTimeRaise() {
        return sumCarsRemainedInQueues() / 2;
    }

    public void changeColor() {

        // if enters this function, it should commute, meaning take a decision based on QueuesLen
        if (canCommute()) {

            // improve superior time
            if (sumCarsRemainedInQueues() > 0) {
                long timeRaise = computeTimeRaise();
                if (this.maxTime + timeRaise < Globals.maxTrafficLightTime) {
                    this.maxTime += timeRaise;
                }

            }

            // after the last big queue that passed, switching time between lights should be the normal one
            // cannot risk to give green light to nobody,then 5 cars are going to the red tf and wait for nothing
            if (waitingQueue.size() == 0 && decidedTime > maxTime) {
                decidedTime = normalTime;
            }

            // minimum decided time is time to pass for a single car; we can check by maximumCheckPhases=4 times to see
            // if there are cars that coming and, if there are not, we should reset to normal. Energy argument.
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
                long allCarsStopped = 0;
                List<Integer> longQueues = new ArrayList<>();
                int queueLenCanPass = (int) (this.maxTime / Globals.passIntersectionTime);

                synchronized (lockQueue) {
                    for (Pair<Long, Integer> key : waitingQueue.keySet()) {
                        int currentQueueLen = waitingQueue.get(key).getSecond();
                        allCarsStopped += currentQueueLen;

                        if (currentQueueLen > maxQueueLen) {
                            maxQueueLen = currentQueueLen;
                        }

                        if (queueLenCanPass < currentQueueLen) {
                            longQueues.add(currentQueueLen);
                        }
                    }
                }

                if (allCarsStopped > 0) {

                    if (longQueues.isEmpty()) {
                         decidedTime = maxQueueLen * Globals.passIntersectionTime;
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
                waitingQueue.clear(); // SmartTrafficLight made a decision based on Queue dimension
            }

            this.lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
            switchLightGroups();
            this.shouldChangeColor = true;
        } else if (receivedEmergencySignal) {
            this.lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
            this.decidedTime = (long) (Globals.maxTrafficLightTime * 1.3);
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

            // TODO !!! sent notification to car with an estimated time to wait in order to adapt speed !!
            // neglect propagation time from sending the message to the time it is received
            Pair<Long, Integer> firstCarInQueue = new Pair<>(data.getTimeStop(), 1);
            waitingQueue.put(street, firstCarInQueue);

        } else {

            // increment queue size and keep waiting time of the first car arrived
            Pair<Long, Integer> updatedQueue =
                    new Pair<>(waitingQueue.get(street).getFirst(), waitingQueue.get(street).getSecond() + 1);
            waitingQueue.put(street, updatedQueue);
        }
        int a = 10;
        /** TODO !!! PROCESS THIS DATA
         * data.getMapPoint(),
         * data.getWayId(),
         * data.getDirection(),
         * waitingQueue.get(key).getSecond());*/
    }

    public void setGreenForEmergency(ApplicationTrafficLightControlData data) {
        if (isEmergencyMode == false) {
            receivedEmergencySignal = true;
            tlvEmergency = findTrafficLightByWay(data.getWayId());
        }
    }

    public int getRedGroupId() {
        if (greenGroup == 0)
            return 1;
        return 0;
    }

    public int getGreenGroupId() {
        if (greenGroup == 0)
            return 0;
        return 1;
    }

    public void switchLightGroups() {
        if(greenGroup == 0) {
            greenGroup = 1;
        } else {
            greenGroup = 0;
        }
    }

    public String runApplications() {
        String result = "";
        for (Application application : this.applications) {
            result += application.run();
        }
        return result;
    }
}
