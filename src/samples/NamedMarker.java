package samples;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.MapMarkerCircle;

/**
 * Class used to identify a MapMarkerCircle by a given name and coordinates.
 * @author Marian
 *
 */

class NamedMarker extends MapMarkerCircle {

    private static final double RADIUS = 5;

    public NamedMarker(String name, Coordinate coordinate) {
        super(null, name, coordinate, NamedMarker.RADIUS, STYLE.FIXED, getDefaultStyle());
    }
}