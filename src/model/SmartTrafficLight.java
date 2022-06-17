package model;

import application.Application;
import application.trafficLight.ApplicationTrafficLightControlData;
import controller.newengine.SimulationEngine;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import model.mobility.MobilityEngine;
import model.parameters.Globals;
import utils.Pair;

import java.util.*;

public class SmartTrafficLight extends TrafficLightModel {

    private Integer lockQueue = 1;
    private MobilityEngine mobilityEngine;

    // tells what group is the green in a moment of time
    private boolean complementarySwitching = true;
    private int greenGroup;
    private Pair<List<TrafficLightView>, List<TrafficLightView>> complementaryGroups;
    private List<Integer> carsRemainedInQueue;

    private static long maxTime = 50;
    private long minTime = 10;

    private long lastTimeUpdate;
    private long decidedTime;
    private boolean shouldChangeColor;

    private boolean isEmergency = false;
    private boolean emergencyMode = false;
    TrafficLightView tlvEmergency = null;

    private void initComplementaryPairs() {
        greenGroup = 0;
        complementaryGroups = new Pair(new LinkedList<>(), new LinkedList<>());
        carsRemainedInQueue =  new ArrayList<>(Collections.nCopies(2, 0));
    }

    public SmartTrafficLight(long id, Node node) {
        super(id, node);
        this.lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
        this.decidedTime = this.minTime;
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
                complementaryGroups.getFirst().add(trafficLight);
            }
        }
    }

    private List<TrafficLightView> getLightsByColor(String color) {
        if (greenGroup == 0 && color.equals("green")) return complementaryGroups.getFirst();
        if (greenGroup == 0 && color.equals("red")) return complementaryGroups.getSecond();
        if (greenGroup == 1 && color.equals("red")) return complementaryGroups.getFirst();
        if (greenGroup == 1 && color.equals("green")) return complementaryGroups.getSecond();
        return null;
    }

    // base scenario
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

            if (emergencyMode) {
                // At this moment all trafficLights should be red
                tlvEmergency.updateTrafficLightView();
                // this are the greenLights which were turn off when ambulance came
                getLightsByColor("green").forEach(redTFV -> {
                    redTFV.updateTrafficLightView();
                });
                emergencyMode = false;
            }

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
        } else if (isEmergency) {
            this.lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
            this.decidedTime = Globals.maxTrafficLightTime * 3;
        }
    }

    public void updateTrafficLightViews() {
        if (shouldChangeColor) {
            trafficLightViewList.forEach(tf -> tf.updateTrafficLightView());
            this.shouldChangeColor = false;
        }

        if (isEmergency) {
            tlvEmergency.updateTrafficLightView();
            getLightsByColor("green").forEach(greenTLV -> {
                greenTLV.updateTrafficLightView();
            });
            isEmergency = false;
            emergencyMode = true;
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
        Pair<Long, Integer> priorityStreet = new Pair<>(data.getWayId(), data.getDirection());
        isEmergency = true;
        tlvEmergency = findTrafficLightByWay(priorityStreet.getFirst());
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
