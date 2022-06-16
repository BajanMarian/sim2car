package application.trafficLight;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import model.GeoTrafficLightMaster;
import model.TrafficLightModel;
import model.network.Message;
import model.network.MessageType;
import model.parameters.Globals;
import utils.tracestool.parameters.GenericParams;
import application.Application;
import application.ApplicationType;
import controller.network.NetworkInterface;
import controller.network.NetworkType;

/**
 * This class is used to simulated the behavior of the traffic light master from the traffic light
 * control app point of view
 * It adds the car (the sender of the message) to the corresponding waiting queue.
 * The car sends a message to the traffic light master only when the traffic light is red.
 * @author Andreea
 *
 */
public class ApplicationTrafficLightControl extends Application {

	private final static Logger logger = Logger.getLogger(ApplicationTrafficLightControl.class.getName());

	/* The type of application  */
	private ApplicationType type = ApplicationType.TRAFFIC_LIGHT_CONTROL_APP;

	/* Reference to the traffic light master object */
	TrafficLightModel trafficLightMaster;
	
	/* key = trafficLightMasterId value = (avg_waiting_time, avg_queue_length) */
	public static TreeMap<Long, String> queuesStatistics = new TreeMap<Long, String>();

	/* If it demands a route or not */
	public boolean isActive = false;

	public ApplicationTrafficLightControl(TrafficLightModel trafficLightMaster){
		this.trafficLightMaster = trafficLightMaster;
	}

	@Override
	public boolean getStatus() {
		return isActive;
	}

	@Override
	public String run() {
		NetworkInterface net = trafficLightMaster.getNetworkInterface(NetworkType.Net_WiFi);
		net.processOutputQueue();
		return "";
	}

	@Override
	public String stop() {
		return null;
	}

	@Override
	public String getInfoApp() {
		return null;
	}

	@Override
	public Object getData() {
		return null;
	}

	@Override
	public ApplicationType getType() {
		return type;
	}

	/***
	 * Process the message received from a car. Add the car to the corresponding waiting queue
	 */
	@Override
	public void process(Message m) {

		if (m.getType() == MessageType.ADD_WAITING_QUEUE) {
			ApplicationTrafficLightControlData data = (ApplicationTrafficLightControlData)m.getPayload();
			if( data != null ) {
				trafficLightMaster.addCarToQueue(data);
			}
		}
	}
	
	public static void writeWaitingTimeStatistics() {
		PrintWriter writer = null;
		try {

			String fileSpecifier = null;

			if (Globals.useDynamicTrafficLights) {
				fileSpecifier = "_waitingTime&QueueLength_withDynamicTrafficLights.txt";
			} else if (Globals.useTrafficLights) {
				fileSpecifier = "_waitingTime&QueueLength_withTrafficLights.txt";
			}

			if(fileSpecifier != null) {
				writer = new PrintWriter(GenericParams.mapConfig.getCity() + fileSpecifier, "UTF-8");
			} else {
				throw new Exception("Could not write statistics about waiting time in ApplicationTrafficLightControl");
			}

			final String resultsExplanation = "trafficLightMasterId avg_waiting_time[sec] avg_queue_length";
			writer.println(resultsExplanation);

			for (Map.Entry<Long, String> entry : queuesStatistics.entrySet()) {
				writer.println(entry.getKey() + "	" +entry.getValue());
			}

			writer.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void stopGlobalApplicationActions(){
		writeWaitingTimeStatistics();
	}
	
	public static void saveData(long trafficLightId, double avg_waitingTime, double avg_queueLength) {
		queuesStatistics.put(trafficLightId, avg_waitingTime + "	" + avg_queueLength);
	}
}
