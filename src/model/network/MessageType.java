package model.network;

public enum MessageType {

	/* Used by TilesApplication */
	REQUEST_ROUTE_TILES_PEER,
	REQUEST_CRTPOS_PROXIMITY_PEER,
	REQUEST_ROUTE_TILES_SERVER,

	/* Used by RoutingApplication */
	REQUEST_ROUTE_UPDATE,
	NEW_ROUTE_UPDATE,
	NO_ROUTE_UPDATE,
	COST_ROAD_UPDATE,
	SERVER_UPDATE,
	FUEL_UPDATE,
	UNKNOWN,

	/* Used by TrafficLightControlApplication */
	ADD_WAITING_QUEUE,
	REMOVE_WAITING_QUEUE,

	/* Used by SynchronizeIntersectionsApplication */
	SYNCHRONIZE_TRAFFIC_LIGHTS
}