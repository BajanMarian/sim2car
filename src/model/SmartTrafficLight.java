package model;

import model.mobility.MobilityEngine;

public class TrafficLightMaster extends Entity {

    MobilityEngine mobilityEngine;


    public TrafficLightMaster(long id) {
        super(id);
        mobilityEngine = MobilityEngine.getInstance();
    }
}
