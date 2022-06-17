package model.threadpool.tasks;

import model.TrafficLightModel;

public class TrafficLightChangeColor implements Runnable{

	private TrafficLightModel trafficLight;
	
	public TrafficLightChangeColor(TrafficLightModel trafficLight) {
		
		this.trafficLight = trafficLight;
	}
	
	@Override
	public void run() {
		this.trafficLight.changeColor();
	}
}
