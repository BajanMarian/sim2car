package utils.analysis;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TrafficLightsStatsCompare {

    public static void main(String[] args) throws IOException {

        String cwd = System.getProperty("user.dir");

        String baseFile = "1798_routes_time_";
        String extension = ".txt";

        BufferedReader reader;
        String line;
        List<Integer> result = new ArrayList<>();

        int totalDTL = 0;
        int totalSmart = 0;
        int totalTL = 0;

        for (int i = 0; i < 2000; i++) {

            String filename = baseFile + i + extension;
            List<Integer> dtl = new ArrayList<>();
            List<Integer> smart = new ArrayList<>();
            List<Integer> tl = new ArrayList<>();

            String dir = "beijing";

//            reader = new BufferedReader(new FileReader(Paths.get(cwd,"rome", "TL", filename).toString()));
//            while ((line = reader.readLine()) != null) {
//                tl.add(Integer.parseInt(line.split(" ")[1]));
//            }

            reader = new BufferedReader(new FileReader(Paths.get(cwd,dir, "DTL", filename).toString()));
            while ((line = reader.readLine()) != null) {
                dtl.add(Integer.parseInt(line.split(" ")[1]));
            }

            reader = new BufferedReader(new FileReader(Paths.get(cwd, dir, "SMART_TL", filename).toString()));
            while ((line = reader.readLine()) != null) {
                smart.add(Integer.parseInt(line.split(" ")[1]));
            }


            //int sizeMin = Math.min(Math.min(dtl.size(), tl.size()), smart.size());
            int sizeMin = Math.min(dtl.size(), smart.size());

            for (int k = 0; k < sizeMin; k++) {
                //if (dtl.get(k) > 50 || smart.get(k) > 50)
                    result.add(dtl.get(k) - smart.get(k));
                    totalDTL += dtl.get(k);
                    totalSmart += smart.get(k);
                    //totalTL += tl.get(k);
            }

        }

        int positive_values = 0;
        int negative_values = 0;
        int equals = 0;
        int winTime = 0;
        int lostTime = 0;

        for(Integer dif: result) {
            if (Math.abs(dif) > 0) {

                if(dif > 0) {
                    winTime += dif;
                    positive_values ++;
                }

                if(dif < 0) {
                    lostTime += dif;
                    negative_values++;
                }

            } else {
                equals ++;
            }

        }

        double winTimeResult = winTime + lostTime;
        double total_samples = positive_values + negative_values;

        System.out.println("TOTAL ROUTES: TL " + totalTL + " vs DTL " + totalDTL + " vs Smart " + totalSmart);
        System.out.println("AGAINST DTL: WIN_TIME_WITH_SMART " + winTime + " vs " + "LOST_TIME_WITH_SMART " + lostTime);
        System.out.println("AGAINST DTL: WIN_ROUTES_WITH_SMART  " + positive_values + " vs " + "LOST_ROUTES_WITH_SMART " + negative_values);
        System.out.println("EQUAL ROUTES REGARDING TIME: " + equals);
        System.out.println("TOTAL_WIN_TIME " + winTimeResult);
        System.out.println("WIN_TIME_PER_ROUTE " + winTimeResult / total_samples);

        double waitTimeDTL = 0;
        String statsWaitingTimeDTL = "beijing_waitingTime&QueueLength_withDynamicTrafficLights.txt";
        String statsWaitingTimeSMART = "beijing_waitingTime&QueueLength_withSmartTL.txt";
        double waitTimeSmart = 0;

        reader = new BufferedReader(new FileReader(Paths.get(cwd,statsWaitingTimeDTL).toString()));
        reader.readLine();
        line = reader.readLine();
        String delim = String.valueOf(line.charAt(4)); //  depends on number of cars and ids charAt(4) vs charAt(3)

        waitTimeDTL += Double.parseDouble(line.split(delim)[1]);
        while ((line = reader.readLine()) != null) {
            waitTimeDTL += Double.parseDouble(line.split(delim)[1]);
        }

        reader = new BufferedReader(new FileReader(Paths.get(cwd,statsWaitingTimeSMART).toString()));
        reader.readLine();
        while ((line = reader.readLine()) != null) {
            waitTimeSmart += Double.parseDouble(line.split(delim)[1]);
        }

        System.out.println("DTL waiting time " + waitTimeDTL);
        System.out.println("SMART waiting time " + waitTimeSmart);
    }
}
