package model.threadpool.tasks;

import model.GeoTrafficLightMaster;
import model.TrafficLightModel;

public class TrafficLightApplicationsRun implements Runnable {

	private TrafficLightModel trafficLightMaster;
	
	public TrafficLightApplicationsRun(TrafficLightModel trafficLightMaster) {
		this.trafficLightMaster = trafficLightMaster;
	}
	
	@Override
	public void run() {
		this.trafficLightMaster.runApplications();
	}
}
