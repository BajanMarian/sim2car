package controller.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import model.Entity;
import model.network.Message;

/*
 * Network interface used by Wi-Fi
 */
public abstract class NetworkInterface {

	private NetworkType type;
	private Entity owner;
	private List<Message> inputQueue;
	private List<Message> outputQueue;

	public NetworkInterface(NetworkType type) {
		this.type = type;
		/* using synchronizedList for multithreading; a car can receive another message
		 while processing the current messages queue; item for output queue */
		this.inputQueue = Collections.synchronizedList(new LinkedList<Message>());
		this.outputQueue = Collections.synchronizedList(new LinkedList<Message>());
	}

	/** Method used to empty the internal outputQueue by sending all messages */
	public abstract void processOutputQueue();
	public abstract Message getNextInputMessage();
	public abstract ArrayList<NetworkInterface> discoversServers();
	public abstract ArrayList<NetworkInterface> discoversPeers();
	public abstract NetworkInterface discoverClosestServer();

	/**
	 * Method used to send message to another NetworkInterface
	 * @param message	Message which needs to be sent to destination NetworkInterface
	 * @param dest		Receiver NetworkInterface
	 */
	public void send(Message message, NetworkInterface dest) {
		dest.receive(message);
	}

	/**
	 * Method used to receive a new message from other NetworkInterface and process it.
	 * @param message	message
	 */
	public void receive(Message message) {
		owner.process(message);
	}

	/**
	 * Enqueue message to outputQueue for sending to its destination entity
	 * @param message	message object placed to be sent; it will be serialized when we will send it.
	 *                  This gives the liberty to look at the message header without serializing it again
	 */
	public void putMessage(Message message) {
		outputQueue.add(message);
	}

	/**
	 * Methods used to stop NetworkInterface by returning an empty string ("")
	 * @return empty string
	 */
	public String stop() {
		return "";
	}

	public NetworkType getType() {
		return this.type;
	}

	public Entity getOwner() {
		return owner;
	}

	public void setOwner(Entity entity) {
		owner = entity;
	}

	public List<Message> getInputQueue() {
		return this.inputQueue;
	}

	public List<Message> getOutputQueue() {
		return this.outputQueue;
	}

}

