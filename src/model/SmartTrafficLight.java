package model;

import application.trafficLight.ApplicationTrafficLightControlData;
import controller.newengine.EngineUtils;
import controller.newengine.SimulationEngine;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import model.OSMgraph.Way;
import model.mobility.MobilityEngine;
import model.parameters.Globals;
import utils.Pair;

import java.util.*;

public class SmartTrafficLight extends Entity {

    /** the cell where the trafficLights is on osm */
    private final Node emplacement;

    /** The smartTrafficLight takes the decision which trafficLights from intersection will become green or red */
    private List<TrafficLightView> trafficLightSlaves;

    /** There can be one-way or two-way streets. A street is defined by a 'wayID' and its 'direction'.
     *  The Pair which stands as a value for TreeMap represents 'waitingTime' of a queue of cars
     *  coming from a direction. The 'total number of cars' in the queue is also stored in this data structure.
     *  PAIR_1 = (wayID, direction)
     *  PAIR_2 = (waitingTime, queueLength)
     * */
    private TreeMap<Pair<Long, Integer>, Pair<Long, Integer>> waitingQueue;

    /** keeps last time a slave traffic light was updated */
    private TreeMap<TrafficLightView, Long> updatesTimestamps;

    private MobilityEngine mobilityEngine;

    public SmartTrafficLight(long id, Node node) {
        super(id);
        this.emplacement = node;
        this.mobilityEngine = MobilityEngine.getInstance();
        this.trafficLightSlaves = new ArrayList<>();
        this.waitingQueue = new TreeMap<>();
    }

    public Node getLocation() {
        return this.emplacement;
    }

    void addTrafficLightSlave(TrafficLightView trafficLight) {
        this.trafficLightSlaves.add(trafficLight);
        this.updatesTimestamps.put(trafficLight, SimulationEngine.getInstance().getSimulationTime());
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
         *waitingQueue.get(key).getSecond());*/
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

    public boolean containsTrafficLightByWay(long wayId) {
        for (TrafficLightView trafficLightView : trafficLightSlaves) {
            if (trafficLightView.getWayId() == wayId)
                return true;
        }
        return false;
    }

    // retrieve
    public Pair<Long, Integer> findWayByQLength(int i) {

        for (Pair<Long, Integer> streetDirection : waitingQueue.keySet()) {
            if(streetDirection.getSecond() == i) {
                return streetDirection;
            }
        }

        return null;
    }
    public void changeColor() {

        if(Globals.useDynamicTrafficLights) {

            Pair<Long, Integer> winnerTrafficLight;
            List<Integer> waitingCars = new ArrayList<>();

            for (Pair<Long, Integer> streetDirection : waitingQueue.keySet()) {
                // places queue size
                waitingCars.add(streetDirection.getSecond());
            }

            int carsNumber = exactlyOneQueueHasCars(waitingCars);
            if (carsNumber != -1) {
                // find that queue id and set that TF_slave as winner
                Pair<Long, Integer> way = findWayByQLength(carsNumber);
                Long wayId = way.getFirst();
                Integer direction = way.getSecond();
                if (containsTrafficLightByWay(wayId));

            }

        }
    }
}
