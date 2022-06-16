package model;

import application.trafficLight.ApplicationTrafficLightControlData;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public abstract class TrafficLightModel extends Entity{

    public abstract void changeColor();
    public abstract void addCarToQueue(ApplicationTrafficLightControlData data);
    public abstract void addNode(Node n);

    public Node emplacement;

    /** traffic lights from the intersection */
    public List<TrafficLightView> trafficLightViewList;

    /** There can be one-way or two-way streets. A street is defined by a 'wayID' and its 'direction'.
     *  The Pair which stands as a value for TreeMap represents 'waitingTime' of a queue of cars
     *  coming from a direction. The 'total number of cars' in the queue is also stored in this data structure.
     *  PAIR_1 = (wayID, direction)
     *  PAIR_2 = (waitingTime, queueLength)
     * */
    public TreeMap<Pair<Long, Integer>, Pair<Long, Integer>> waitingQueue;

    public TrafficLightModel(long id, Node emplacement) {
        super(id);
        this.emplacement = emplacement;
        this.trafficLightViewList = new ArrayList<>();
        this.waitingQueue = new TreeMap<>();
    }

    public void addTrafficLightView(TrafficLightView trafficLightView) {
        this.trafficLightViewList.add(trafficLightView);
    }

    public boolean containsTrafficLightByWay(long wayId) {
        for (TrafficLightView trafficLightView : trafficLightViewList) {
            if (trafficLightView.getWayId() == wayId)
                return true;
        }
        return false;
    }

    public Node getNode() {
        return emplacement;
    }

    public void setNode(Node node) {
        this.emplacement = node;
    }

}
