package model;

import application.trafficLight.ApplicationTrafficLightControlData;
import controller.newengine.EngineUtils;
import controller.newengine.SimulationEngine;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import model.mobility.MobilityEngine;
import model.parameters.Globals;
import utils.Pair;

import java.util.*;

public class SmartTrafficLight extends TrafficLightModel {

    private Integer lockQueue = 1;

    /** keeps last time a slave traffic light was updated */
    private TreeMap<TrafficLightView, Long> updatesTimestamps;

    private MobilityEngine mobilityEngine;

    // tells what group is the green in a moment of time
    private boolean complementarySwitching = true;
    private int greenGroup;
    private Pair<List<TrafficLightView>, List<TrafficLightView>> complementaryGroups;
    private List<Integer> carsRemainedInQueue;
    private static long minTimeBeforeChange = 10;
    private long maximumTimeGreenLight = 60;

    // the amount of time for a light to be read and green.
    private static double oneCycle = 100;
    // the maximum amount of time group one can stay on green
    private double countGreenColorGroupOne = 50;

    private long lastChanged;

    private void initComplementaryPairs() {
        greenGroup = 0;
        complementaryGroups = new Pair(new LinkedList<>(), new LinkedList<>());
        carsRemainedInQueue =  new ArrayList<>(Collections.nCopies(2, 0));
    }

    public SmartTrafficLight(long id, Node node) {
        super(id, node);
        this.lastChanged =  SimulationEngine.getInstance().getSimulationTime();
        this.mobilityEngine = MobilityEngine.getInstance();
        initComplementaryPairs();
    }

    public Node getLocation() {
        return this.emplacement;
    }

    public void addTrafficLightSlave(TrafficLightView trafficLight) {
        this.trafficLightViewList.add(trafficLight);
        this.updatesTimestamps.put(trafficLight, SimulationEngine.getInstance().getSimulationTime());

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

    public double getGreenLightTimeByGroup(int id) {
        if(id == 0) {
            return countGreenColorGroupOne;
        } else {
            return oneCycle - countGreenColorGroupOne;
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

    public void changeLights() {
        if(greenGroup == 0) {
            greenGroup = 1;
        } else {
            greenGroup = 0;
        }
    }

    public boolean canCommute() {
        if (SimulationEngine.getInstance().getSimulationTime() - this.lastChanged > this.minTimeBeforeChange) {
            return true;
        }
        return false;
    }

    private int sumCarsRemainedInQueues() {
        int total = carsRemainedInQueue.stream().mapToInt(noCars -> noCars).sum();
        return total;
    }

    public void changeColor() {

        if (canCommute()) {

            // improve superior time
            if( sumCarsRemainedInQueues() > 0) {
                this.maximumTimeGreenLight += (sumCarsRemainedInQueues() / 2);
            }

            if (Globals.useDynamicTrafficLights && complementarySwitching) {

                // Track the sum of all waiting queues; also keep the biggest queue length
                /*for (TrafficLightView tf : getLightsByColor("red")) {
                    Long tfStreet = tf.getWayId();
                    Integer tfStreetDir = tf.getDirection();*/

                long biggestQueueLen = -1;
                long sumAllQueues = 0;
                List<Integer> bigQueues = new ArrayList<>();
                long switchTime = minTimeBeforeChange;

                synchronized (lockQueue) {
                    for (Pair<Long, Integer> key : waitingQueue.keySet()) {

                        if (key.getSecond() > biggestQueueLen) {
                            biggestQueueLen = key.getSecond();
                        }

                        if (this.maximumTimeGreenLight / Globals.passIntersectionTime < key.getSecond()) {
                            bigQueues.add(key.getSecond());
                        }

                        sumAllQueues += key.getSecond();
                    }
                }

                if (sumAllQueues > 0) {
                    if (bigQueues.isEmpty()) {
                        switchTime = biggestQueueLen * Globals.passIntersectionTime;
                    } else {

                        switchTime = this.maximumTimeGreenLight;
                        Integer maxCarCanPass = Math.toIntExact((this.maximumTimeGreenLight / Globals.passIntersectionTime));
                        int redGroupId = getRedGroupId();

                        bigQueues.forEach(noCars -> {
                            Integer val = carsRemainedInQueue.get(redGroupId);
                            val += noCars - maxCarCanPass;
                            carsRemainedInQueue.set(redGroupId, val);
                        });
                    }
                }
                waitingQueue.clear(); // SmartTrafficLight made a decision based on Queue dimension
            }

            this.lastChanged = SimulationEngine.getInstance().getSimulationTime();
            trafficLightViewList.forEach(tf -> tf.changeColor());
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

        /** TODO !!! PROCESS THIS DATA
         * data.getMapPoint(),
         * data.getWayId(),
         * data.getDirection(),
         * waitingQueue.get(key).getSecond());*/
    }

    // 3 timpi - (simulator, de_cand_asteapta_prima_masina, de_cand_nu_s-a mai schimbat un semafor)
    // conteaza si dimensiunea cozilor
    // Conteaza numarul de unitati care trec

    /**
     * Checks if an intersection has exactly one queue with cars
     * @param   waitingCars a list with numberOfCars waiting on each queue
     * @return  number of Cars in that queue or
     *          -1 if two queues have cars or there is no car waiting
     */
    public int exactlyOneQueueHasCars(List<Integer> waitingCars) {
        int numberCars = -1;
        for(Integer queueLen : waitingCars) {
            if (queueLen > 0 && numberCars == -1) {
                numberCars = queueLen;
            } else if (queueLen > 0) {
                // return false if 2 queues have cars
                return -1;
            }
        }

        return numberCars;
    }

    private List<TrafficLightView> getTLbyWay(long wayId) {
        List<TrafficLightView> trafficLightList = new ArrayList<>();
        for (TrafficLightView slave: trafficLightViewList) {
            if(slave.getWayId() == wayId) {
                trafficLightList.add(slave);
            }
        }

        if(!trafficLightList.isEmpty()) {
            return trafficLightList;
        }
        return null;
    }
}
