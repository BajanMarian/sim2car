package main;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import model.parameters.Globals;

import controller.engine.EngineInterface;
import controller.newengine.SimulationEngine;
import downloader.Downloader;

public class Main {

	/** Logger used by this class */
	private static final transient Logger logger = Logger.getLogger(Main.class.getName());

	public static String[] args;

	static EngineInterface simulator;

	public static void main(String[] args) {

		/* Read the simulators configuration file */
		try {
			FileInputStream fis =  new FileInputStream("src/configurations/logging.properties");
			LogManager.getLogManager().readConfiguration(fis);
			fis.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}

		Main.args = args;

		/* Activates all the designated parameters for the simulator to be run with */
		Globals.setUp( args );
		if (Globals.propertiesFile == null) {
			// use -prop src\configurations\simulator\rome.properties for e.g.
			logger.severe("option -prop is mandatory");
			System.exit(0);
		}

		// Download the traces
		Downloader.getInstance().downloadTraces(Globals.propertiesFile);

		/* enable proxy connection if settings are present */
		utils.Proxy.checkForProxySettings();

		simulator = SimulationEngine.getInstance();
		simulator.setUp();
		simulator.start();
	}
}
