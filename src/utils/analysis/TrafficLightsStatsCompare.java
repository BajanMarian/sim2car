package utils.analysis;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TrafficLightsStatsCompare {

    public static void main(String[] args) throws IOException {

        String cwd = System.getProperty("user.dir");
        List<String> filesName = new ArrayList<>();
        String baseFile = "1798_routes_time_";
        String extension = ".txt";

        BufferedReader reader;
        String line;

        List<Integer> result = new ArrayList<>();
        long allROUTES = 0;

        for (int i = 0; i < 250; i++) {
            //int j = 106;
            String filename = baseFile + i + extension;
            List<Integer> dtl = new ArrayList<>();
            List<Integer> smart = new ArrayList<>();
            reader = new BufferedReader(new FileReader(Paths.get(cwd,"rome/DTL/", filename).toString()));
            while ((line = reader.readLine()) != null) {
                dtl.add(Integer.parseInt(line.split(" ")[1]));
            }

            reader = new BufferedReader(new FileReader(Paths.get(cwd,"rome/DTL_me/", filename).toString()));
            while ((line = reader.readLine()) != null) {
                smart.add(Integer.parseInt(line.split(" ")[1]));
            }

            int sizeMin = dtl.size() < smart.size() ? dtl.size() : smart.size();
            for (int k = 0; k < sizeMin; k++) {
                result.add( - smart.get(k) + dtl.get(k));
                allROUTES += smart.get(k);
                allROUTES += dtl.get(k);
            }

        }

        int positive = 0;
        int total_pos = 0;
        int neg = 0;
        int total_neg = 0;
        for(Integer dif: result) {
            if(dif > 0 && dif > 5) {
                total_pos += dif;
                positive++;
            }
            if(dif < 0 && dif <  -5) {
                total_neg += dif;
                neg++;
            }
        }

        System.out.println("WIN_TIME " + total_pos);
        System.out.println("LOST_TIME " + total_neg);
        System.out.println("POS_VAL " + positive);
        System.out.println("NEG_VAL" + neg);

        double total = total_pos + total_neg;
        double total_samples = neg + positive;

        System.out.println("TIMP CASTIGAT PER TOTAL " + (total_pos + total_neg));
        System.out.println("DURATA MEDIE_RUTA " +  allROUTES / total_samples);
        System.out.println("RUTA castig " + total / total_samples);




        BufferedWriter writer = new BufferedWriter(new FileWriter("trafficLightsFiles_irregularities.txt"));


    }
}
