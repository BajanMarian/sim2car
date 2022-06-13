package model.OSMgraph;

import java.io.Serializable;

/**
 * <Node> is used to represent a node from OSM file.
 * Besides its id, wayId and coordinates, there is a check for existence of a
 * trafficLight inside it. The trafficLight id is also stored.
 */
public class Node implements Comparable<Node>, Serializable {

	private static final long serialVersionUID = -3397967186656052290L;

	public long id, wayId;
	public double lat, lon;

	/* trafficLight id */
	private Long trafficLightControl = null;

	public Node(long id, double lat, double lon) {
		this(id, lat, lon, 0);
	}
	
	public Node(long id, double lat, double lon, long wayId) {
		this.id = id;
		this.lat = lat;
		this.lon = lon;
		this.wayId = wayId;
	}
	
	public void setWayId(long id) {
		wayId = id;
	}
	
	public long getWayId() {
		return wayId;
	}
	
 	@Override
	public boolean equals(Object obj) {

	   if (!(obj instanceof Node)) {
		   return false;
	   }

	   if (obj == this) {
		   return true;
	   }

	   Node node = (Node) obj;
	   return (node.id == this.id);

	}
	
	@Override
	public String toString() {
		return "Node: id = " + id + ", (lat, lon) = (" + lat + " "+ lon + "), wayId = " + wayId + ")\n";
	}

	@Override
	public int compareTo(Node o) {
		return (int)(this.id - o.id);
	}
	
	public boolean hasTrafficLightControl() {
		return trafficLightControl == null ? false : true;
	}
	
	public Long getTrafficLightId() {
		return trafficLightControl;
	}
	
	public void setTrafficLightControl(Long trafficLightControl) {
		this.trafficLightControl = trafficLightControl;
	}
	
	
}