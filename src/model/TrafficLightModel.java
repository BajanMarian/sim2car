package model;

import application.trafficLight.ApplicationTrafficLightControlData;
import gui.TrafficLightView;
import model.OSMgraph.Node;
import utils.Pair;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public abstract class TrafficLightModel extends Entity{

    public abstract void changeColor();
    public abstract void addCarToQueue(ApplicationTrafficLightControlData data);
    public abstract void addTrafficLightView(TrafficLightView trafficLightView);
    public abstract String runApplications();

    public Node emplacement;

    /** traffic lights from the intersection */
    public List<TrafficLightView> trafficLightViewList;

    private List<Node> nodes = new ArrayList<Node>();

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

    public boolean containsTrafficLightByWay(long wayId) {
        for (TrafficLightView trafficLightView : trafficLightViewList) {
            if (trafficLightView.getWayId() == wayId)
                return true;
        }
        return false;
    }

    public TrafficLightView findTrafficLightByWay(long wayId, long direction) {
        for (TrafficLightView trafficLightView : trafficLightViewList) {
            if (trafficLightView.getWayId() == wayId && trafficLightView.getDirection() == direction)
                return trafficLightView;
        }
        return null;
    }

    public TrafficLightView findTrafficLightByWay(long wayId) {
        for (TrafficLightView trafficLightView : trafficLightViewList) {
            if (trafficLightView.getWayId() == wayId)
                return trafficLightView;
        }
        return null;
    }

    public Color getTrafficLightColor(long wayId, int direction) {
        for (TrafficLightView trafficLightView : trafficLightViewList) {
            if (trafficLightView.getWayId() == wayId && trafficLightView.getDirection() == direction)
                return trafficLightView.getColor();
        }
        return Color.green;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void addNode(Node n) {
        this.nodes.add(n);
    }

    public Node getNode() {
        return emplacement;
    }

    public void setNode(Node node) {
        this.emplacement = node;
    }

}
