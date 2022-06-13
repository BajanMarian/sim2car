# sim2car
Simulator Sim2Car

Before you run the simulator make sure you have the Traffic Light Config file (e.g for Beijing the file is trafficLights_beijing.txt) added to this folder:
processeddata\maps\XmlBeijing\

## *Applications*
Note that there are several application which needs activation depending on your needs:
- ROUTING
- TILES
- STREET_VISITS
- TRAFFIC_LIGHT_CONTROL
- SYNCHRONIZE_INTERSECTIONS

In order to activate them you should go **Global.java** and set  *activeApps field*.
**ApplicationUtils.java** parses the activeApps, but when it comes about their activation is used distinct methods such as:
1. *activateApplicationTrafficLight(ApplicationType, GeoTrafficLightMaster)* -> TRAFFIC_LIGHT_CONTROL
2. *activateApplicationSynchronizeTrafficLight(ApplicationType, GeoTrafficLightMaster)* -> SYNCHRONIZE_INTERSECTIONS_APP
3. *activateApplicationServer(ApplicationType,GeoServer)* -> ROUTING_APP or TILES_APP

## *Loggers*
        Loggers does not work with all jdks. In order to use them to print information to console you should add the following lines in you classes:
    
    static {
		logger = Logger.getLogger(EngineUtils.class.getName());
		/* uncomment the following line in order to log to console */
		logger.addHandler(new ConsoleHandler());
	}

## *Errors*
        Uses logger in EngineUtils lead to:
            controller.newengine.EngineUtils addApplicationToServer
            INFO:  Failed to create application with type TRAFFIC_LIGHT_CONTROL_APP
        This log SHOULD NOT occur because the traffic_light apllication is is not activated by addApplicationToServer.
        It is a bug in the code.

## *Patterns to search after*
    !!! = might be interesting to search for
    
    