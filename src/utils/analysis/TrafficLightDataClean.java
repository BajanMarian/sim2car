package utils.analysis;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Purpose of this class is to identify irregularities in trafficLights input file
 * (ex. trafficLight_rome.txt)
 * All traffic lights being red or green in an intersection is a mistake, and this class
 * should return lineID where we can find that irregularity.
 */
public class TrafficLightDataClean {

    public static boolean isSameColor(List<String> colors) {
        if (colors.size() == 1 || colors.size() == 0) {
            return false;
        }

        String firstCellColor = colors.get(0);
        for (int i = 1; i < colors.size(); i++) {
            if(!firstCellColor.equals(colors.get(i))) {
                return false;
            }
        }

        return true;
    }

    public static void main(String[] args) throws IOException {

        String cwd = System.getProperty("user.dir");
        List<String> filesName = new ArrayList<>();
        // simulating with a cars and 39 servers, tfID starts at 41
        int offset = 40;

        filesName.add("trafficLights_rome.txt");
        filesName.add("trafficLights_sanfrancisco.txt");
        filesName.add("trafficLights_beijing.txt");

        BufferedWriter writer = new BufferedWriter(new FileWriter("trafficLightsFiles_irregularities.txt"));


        filesName.forEach(fileName -> {
            BufferedReader reader;
            List<Integer> problemLines = new ArrayList<>();
            List<Integer> simulatorIDs = new ArrayList<>();

            try {
                reader = new BufferedReader(new FileReader(Paths.get(cwd, fileName).toString()));
                String line = reader.readLine();

                int count = 1;
                int intersectionID = 0;
                int masterLine = -1;
                List<String> tfColors= new ArrayList<>();

                while (line != null) {
                    if (line.startsWith("master")) {
                        if (isSameColor(tfColors)) {
                            problemLines.add(masterLine);
                            simulatorIDs.add(intersectionID + offset);
                        }
                        tfColors= new ArrayList<>();
                        masterLine = count++;
                        intersectionID++;
                    } else if (line.startsWith("node")) {
                        count++;
                    } else {
                        String[] componets = line.split(" ");
                        tfColors.add(componets[componets.length - 1]);
                        count++;
                    }
                    line = reader.readLine();
                }

                writer.write("-> Please check <" + fileName + "> at lines displayed below! \n");
                writer.write(problemLines.size() + " irregularities found\n");
                writer.write(problemLines + "\n");
                writer.write("=== If you want to see the trafficLights displayed in gui, " +
                        "you can search for these IDs ===\n");
                writer.write(simulatorIDs + "\n\n");

                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        writer.close();
    }
}
