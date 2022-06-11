package samples;

import java.awt.Color;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;

import org.jfree.ui.OverlayLayout;
import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;

/**
 * Class used to view cars' routes by their ids.
 * @author Marian
 *
 */

class CabsProperties {

    private final int id;
    private ArrayList<Coordinate> coordinates;
    private ArrayList<NamedMarker> markers;
    private Color color;
    private int stopRaw = 2000;

    CabsProperties(int id, Color color) {
        this.id = id;
        this.color = color;
        coordinates = new ArrayList<Coordinate>();
        markers = new ArrayList<NamedMarker>();
    }

    public void setSTOP_RAW(int STOP_RAW) {
        this.stopRaw = STOP_RAW;
    }

    public int getSTOP_RAW() {
        return stopRaw;
    }

    public NamedMarker getMarkerByIndex(int index) {
        return this.markers.get(index);
    }

    public Coordinate getCoordinateByIndex(int index) {
        return this.coordinates.get(index);
    }

    public void readCoordinates(String dataFolder) throws IOException {

        String cabFile = this.id + ".txt";
        RandomAccessFile raf = new RandomAccessFile(Paths.get(dataFolder, cabFile).toString(), "r");

        Integer rawsRead = 0;
        String raw;

        while ((raw = raf.readLine()) != null) {

            if (++rawsRead > this.stopRaw)
                break;

            String[] props = raw.split(" ");
            Double pointX = Double.parseDouble(props[0]);
            Double pointY = Double.parseDouble(props[1]);
            Coordinate coordinate = new Coordinate(pointX, pointY);

            /* create custom marker with name and color as visual identifiers */
            NamedMarker marker = new NamedMarker("Cab" + this.id, coordinate);
            marker.setBackColor(this.color);
            marker.setColor(this.color);

            this.coordinates.add(coordinate);
            this.markers.add(marker);
        }
    }
}


public class CabsInRome extends JFrame {

    private static final long serialVersionUID = 1L;
    private JMapViewer map;

    public CabsInRome() {

        super("CabsInRome");
        setLayout(new OverlayLayout());

        final double lat = 41.9072957658043;
        final double lon = 12.4894718722508;

        map = new JMapViewer();
        map.setDisplayPositionByLatLon(lat, lon, 13);
        map.setZoomContolsVisible(true);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 900);
        setLocation(100, 100);

        add(map);
    }

    public void displayCabs(ArrayList<CabsProperties> cabs) throws Exception {

        final int totalSimulationSteps = 100;

        for (int i = 0; i < totalSimulationSteps; i++) {

            map.removeAllMapMarkers();
            int finalI = i;
            cabs.forEach((cab) -> {
                map.addMapMarker(cab.getMarkerByIndex(finalI));
            });

            TimeUnit.MILLISECONDS.sleep(300);
        }

    }

    public static void main(String[] args) throws Exception {

        CabsInRome frame = new CabsInRome();
        frame.setVisible(true);

        String prjDir = System.getProperty("user.dir");
        String dataFolderAbsPath = Paths.get(prjDir, "processeddata/traces/rome/InterpolatedRome/").toString();

        ArrayList<CabsProperties> cabs = new ArrayList<CabsProperties>();

        // add new cars if you are interested in other routes
        cabs.add(new CabsProperties(28, new Color(0, 255, 0)));
        cabs.add(new CabsProperties(37, new Color(0, 255, 255)));

        cabs.forEach((localCab) -> {
            try {
                localCab.readCoordinates(dataFolderAbsPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        frame.displayCabs(cabs);

    }
}

