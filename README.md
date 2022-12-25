# sim2car
Simulator Sim2Car

## *Applications*
Note that there are several applications which needs activation depending on your needs:
- ROUTING
- TILES
- STREET_VISITS
- TRAFFIC_LIGHT_CONTROL
- SYNCHRONIZE_INTERSECTIONS

In order to activate them you should go to **Global.java** and set  *activeApps field*.
**ApplicationUtils.java** parses the activeApps, but when it comes about their activation, there are used distinct methods such as:
1. *activateApplicationTrafficLight(ApplicationType, GeoTrafficLightMaster)* -> TRAFFIC_LIGHT_CONTROL
2. *activateApplicationSynchronizeTrafficLight(ApplicationType, GeoTrafficLightMaster)* -> SYNCHRONIZE_INTERSECTIONS_APP
3. *activateApplicationServer(ApplicationType,GeoServer)* -> ROUTING_APP or TILES_APP

## *How to run the simulator*
Mandatory command line argument: the path to the city properties file. 

    -prop src\configurations\simulator\rome.properties

Before you run the simulator make sure you have the Traffic Light Config file (e.g for Beijing the file is trafficLights_beijing.txt)
added to this folder:processeddata\maps\XmlBeijing\

In Globals.java there is a huge list of arguments that can be used. Depending on your needs, you should
take a look and set those variables accordingly. A few other examples, but not all are listed below:

    --activeApps ROUTING,TRAFFIC_LIGHT_CONTROL
    --carsCount 50                  = limits the number of cars to 50
    --maxNoTrafficLight  100        = limits the number of tf to 100
    --typeOfTrafficLight 1          = 0-GeoTrafficLightMaster 1-SmartTrafficLight

If you want to use traffic lights in your simulation, note that it is mandatory to set one of these variables:

    useTrafficLights = false;
    useDynamicTrafficLights = true;
    GeoTrafficLightMaster implements static traffic lights and dynamic traffic lights.
    SmartTraffucLight only implements dynamic traffic lights.
    You need to set these variables accordingly with "typeOfTrafficLight"

For displaying entities IDs in GUI you should set:

    displayCarsIds = true
    displayTrafficLightsIds = true;

## *Architecture*

    MVC architecture
    SimulationEngine represents the current controller, EngineSimulation being its older version. Its source code is in src/controller/.
    The view is represented by src/gui/ folder, where almost all gui elements are added.


## *Simulator-firstRun*
When running the simulator for the first time, it starts downloading an archive data for the specified city. At the end
of the whole process the simulator will run, and you will have 2 additional folders, **rawdata/** and **processeddata/**.
Files used by simulator are now displayed in the console, so you can see which one are useful.

## *Entity*
At this moment, there are 3 type of entities:
    1.cars
    2.servers
    3.traffic lights

Each entity has an ID and; when you run the simulator, you can see their range ids displayed.

*Cars* have their own routes and are using the ROUTING_APP to find their destination faster. Cars can detect entities like 
traffic lights and other cars. They can adept their speed, got statistics about street crowdedness, stop at traffic lights,
avoid collision with other cars, compute their average speed and fuel consumption.

*Server* also use the ROUTING_APP to redirect the cars on better routes.

*Traffic lights* are defined by a master and additional traffic lights slaves. The master can be a GeoTrafficLightMaster
or a SmartTrafficLight. The slaves are always instances of TrafficLightViews class and are not considered entities. Masters
can communicate between them for synchronizing close intersections. There is also a class called SmartTrafficLightExtended
which represents a traffic light which can have multiple phases.


## *Communication*  
There are three types of communication:
    
    car - server
    car - car
    car - traffic light master

They used messages to communicate via NetworkInterface class. 
There are 2 types of interfaces, Wi-Fi and Bluetooth.
All entities are using the Wifi-Interface. They use a series of messages, as you can see in src/model/network.
Each entity should activate its own interface in order to received message from the other ones. When a message is received,
it is processed and an associated action is executed.


## *Loggers*
Loggers do not work even though you activate the logging params in Globals. In order to use them to print information to console
you should add the following lines in your classes (a consoleHandler should be attached to logger).
    
    static {
		logger = Logger.getLogger(EngineUtils.class.getName());
		logger.addHandler(new ConsoleHandler());
	}

## *Errors*
        Use of logger in EngineUtils leads to:
            controller.newengine.EngineUtils addApplicationToServer
            INFO:  Failed to create application with type TRAFFIC_LIGHT_CONTROL_APP
        This log SHOULD NOT occur because the traffic_light apllication is is not activated by addApplicationToServer.
        It is a bug in the code.

## *Technical notes* 
    1.The project was tested with the followind SDKs: JavaSE-1.8, Azul-1.8, Correto-1.8
    2.When working in your IDE, make sure to mark data and statistics folders as *excluded*. They should not be indexed.

## *Patterns to search for*
    !!! = might be interesting to search for or TODOs
    
    