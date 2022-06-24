package model;

import application.Application;
import application.trafficLight.ApplicationTrafficLightControlData;
import controller.newengine.SimulationEngine;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import model.parameters.Globals;
import utils.Pair;

import java.util.*;

public class SmartTrafficLightExtended extends TrafficLightModel{

    private Integer lockQueue = 1;

    private LinkedList<Integer> orderQueue;
    private List<List<TrafficLightView>> groups;
    // depending on congestion, this times will increase for each group if necessary
    private List<Long> maxTimeLimits;

    private int phases = 0;
    private int maxPhaseNumber;

    private long inferiorLimitMaxTime = 60;
    private long normalTime = 40;
    // if the decided time is checkTime, then all trafficLight stays the same till one car passes by
    private long checkTime = 10;

    private long lastTimeUpdate;
    private long decidedTime;
    private boolean needsRendering;

    // default order is 0, 1, 2, phases -1
    public void initGroups(int phases) {
        groups = new ArrayList<>();
        orderQueue = new LinkedList<>();
        maxTimeLimits = new ArrayList<>();

        for (int i = 0; i < phases; i++) {
            List<TrafficLightView> group = new ArrayList<>();
            groups.add(group);
            orderQueue.add(i);
            maxTimeLimits.add(inferiorLimitMaxTime);
        }
    }

    public SmartTrafficLightExtended(long id, Node emplacement, int phases) {
        super(id, emplacement);
        maxPhaseNumber = phases;
        lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
        decidedTime = normalTime;
        initGroups(phases);
    }

    public boolean canCommute() {
        if (SimulationEngine.getInstance().getSimulationTime() - this.lastTimeUpdate > this.decidedTime) {
            return true;
        }
        return false;
    }

    @Override
    public void changeColor() {

        if (canCommute()) {

            // enters only if there are cars at the red light
            if (Globals.useDynamicTrafficLights && waitingQueue.size() > 0) {

                int maximumChecks = maxPhaseNumber - 1;
                int i;

                synchronized (lockQueue) {

                    // when checking for groups, expect to find at least one queue with cars due to condition from above
                    for (i = 1; i <= maximumChecks; i++) {

                        /* note: that part with all longQueues was removed */

                        orderQueue.addLast(orderQueue.removeFirst());

                        int maxQueueLen = -1;
                        long allCarsStopped = 0; /* could be kept for statistics */
                        int nextId = orderQueue.getFirst();
                        List<TrafficLightView> trafficLightGroup = groups.get(nextId);
                        List<Pair<Long, Integer>> streetsToRemove = new LinkedList<>();

                        // identify those street for the next group of traffic lights
                        for (int j = 0; j < trafficLightGroup.size(); j++) {

                            TrafficLightView tfl = trafficLightGroup.get(j);

                            for (Pair<Long, Integer> key : waitingQueue.keySet()) {

                                if (tfl == findTrafficLightByWay(key.getFirst())) {
                                    int currentQueueLen = waitingQueue.get(key).getSecond();
                                    allCarsStopped += waitingQueue.get(key).getSecond();

                                    if (currentQueueLen > maxQueueLen) {
                                        maxQueueLen = currentQueueLen;
                                    }
                                    streetsToRemove.add(key);
                                }
                            }
                        }

                        // need to remove the streets associated with traffic lights that come next
                        if (!streetsToRemove.isEmpty()) {
                            streetsToRemove.forEach(street -> waitingQueue.remove(street));
                        }


                        // should set light for this queue
                        if (allCarsStopped > 0) {

                            int maxPassingCars = (int) (maxTimeLimits.get(nextId) / Globals.passIntersectionTime);

                            if (maxPassingCars > maxQueueLen) {
                                decidedTime = (long) maxQueueLen * Globals.passIntersectionTime;
                                // slowly decrease
                                if (maxTimeLimits.get(nextId) - Globals.passIntersectionTime > inferiorLimitMaxTime) {
                                    maxTimeLimits.set(nextId, maxTimeLimits.get(nextId) - Globals.passIntersectionTime);
                                }
                            } else {

                                decidedTime = maxTimeLimits.get(nextId);
                                int maxRemainedCars = maxQueueLen - maxPassingCars;
                                int extraTime = maxRemainedCars * Globals.passIntersectionTime;

                                if (extraTime > Globals.maxTrafficLightTime - maxTimeLimits.get(nextId)) {
                                    maxTimeLimits.set(nextId, (long) Globals.maxTrafficLightTime);
                                } else {
                                    // trivial formula
                                    maxTimeLimits.set(nextId, extraTime + maxTimeLimits.get(nextId));
                                }
                            }

                            // set these lights on green
                            trafficLightGroup.forEach(tfl -> {
                                tfl.setColor("green");
                            });
                            // set previous lights on red
                            groups.get(orderQueue.getLast()).forEach(tfl -> {
                                tfl.setColor("red");
                            });

                            lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
                            needsRendering = true;
                            break;
                        } else {
                            groups.get(orderQueue.getLast()).forEach(tfl -> {
                                tfl.setColor("red");
                            });
                        }
                    }
                }
            } else {
                // no queues -> tf stay the same, with the master checking periodically for new cars
                lastTimeUpdate = SimulationEngine.getInstance().getSimulationTime();
                decidedTime = checkTime;
            }
        }
    }

    public void updateTrafficLightViews() {
        /* the traffic lights won't need to change color with the old logic,
         * their values being set above; they only needs to be rendered; this
         * is a proof that simulator is easily extensible on traffic lights part;
         * check the TrafficLightView to see that for these type of tf, the method
         * changeColor is not called
         */
        if (needsRendering) {
            trafficLightViewList.forEach(tf -> tf.updateTrafficLightView());
            needsRendering = false;
        }
    }


    @Override
    public void addTrafficLightView(TrafficLightView trafficLightView) {
        trafficLightViewList.add(trafficLightView);
        try {
            groups.get(trafficLightView.getInternId()).add(trafficLightView);
        } catch (Exception e) {
            System.err.println("Problem in SmartTrafficExtended; Check for the ids set on trafficLightsView and number of phases");
            e.printStackTrace();
        }
        if (trafficLightView.getColorString().equals("green")) {
            orderQueue.remove(Integer.valueOf(trafficLightView.getInternId()));
            orderQueue.addFirst(trafficLightView.getInternId());
        }
    }

    @Override
    public void addCarToQueue(ApplicationTrafficLightControlData data) {

        Pair<Long, Integer> street = new Pair<>(data.getWayId(), data.getDirection());
        if (!waitingQueue.containsKey(street)) {

            Pair<Long, Integer> firstCarInQueue = new Pair<>(data.getTimeStop(), 1);
            waitingQueue.put(street, firstCarInQueue);

        } else {

            Pair<Long, Integer> updatedQueue =
                    new Pair<>(waitingQueue.get(street).getFirst(), waitingQueue.get(street).getSecond() + 1);
            waitingQueue.put(street, updatedQueue);
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
