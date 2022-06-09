package downloader;

import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.parameters.Globals;

/**
 * This class is used to download the traces for the simulator
 * @author Alex
 *
 */
public class Downloader {
	private static Downloader INSTANCE = null;
	private String city;
	
	private Downloader(){}
	
	public static Downloader getInstance() {
		if (Downloader.INSTANCE == null)
			Downloader.INSTANCE = new Downloader();
		
		return Downloader.INSTANCE;
	}
	
	/*
	 * Extract the city name from the give properties file path
	 */
	private void extractCity(String propFile) {

		Pattern filePattern = Pattern.compile("([A-Za-z]*)\\.properties");
		Matcher m = filePattern.matcher(propFile);
		if (m.find()) {
			this.city = m.group(1);
		} else {
			System.err.println("Properties file should have .properties as extension");
		}

		System.out.println("Traffic will be simulated in " + city + "\n");
	}
	
	public void downloadTraces(String propFile) {

		// Extract the city name
		extractCity(propFile);

		if (checkIfTracesExist(this.city)) {
			System.out.println("Skip download step because trace files already exist!\n");
		} else {
			System.out.println("Start download step because trace files cannot be found! Downloading ... \n");

			DownloadCore core = new DownloadCore();
			core.execute(this.city);
		}
	}
	
	/*
	 * Checks if the folders for traces exist for the wanted city
	 */
	private boolean checkIfTracesExist(String city) {
		
		try {
			String cwd = System.getProperty("user.dir");
			String cabsTracesPath = Paths.get(cwd, "rawdata", "traces", city + "cabs").toString();
			String cityTracesPath = Paths.get(cwd, "processeddata", "traces", city).toString();

			File cabsTraces = new File(cabsTracesPath);
			File cityTraces = new File(cityTracesPath);
			
			if (cabsTraces.exists() && cityTraces.exists())
				return true;

		} catch (SecurityException e) {
			System.err.println("You don't have read rights");
			e.printStackTrace();
		}
		
		return false;
	}
}
