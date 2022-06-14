package gui;

import java.awt.Color;
import javax.swing.SwingUtilities;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;

import main.Main;
import model.MapPoint;
import model.parameters.Globals;

public class TrafficLightView {

	private Color trafficLightColor;
	private MapPoint currentPoint;
	private MapMarkerDot lastMk = null;
	private Integer mkLock = 1;

	private long wayId;
	private int direction;

	int groupId;
	JMapViewer map;

	public TrafficLightView(JMapViewer map, MapPoint currentPoint,
			long wayId, int direction) {

		Globals.parseArgs(this, Main.args);
		this.map = map;
		this.trafficLightColor = Color.red;
		this.currentPoint = currentPoint;
		this.wayId = wayId;
		this.direction = direction;
	}

	public TrafficLightView(JMapViewer map, MapPoint currentPoint,
							long wayId, int direction, int groupId) {
		this(map, currentPoint, wayId, direction);
		this.groupId = groupId;
	}

	public MapPoint getCurrentPoint() { return currentPoint; }
	public void setCurrentPoint(MapPoint currentPoint) { this.currentPoint = currentPoint; }

	public long getWayId() { return wayId; }
	public void setWayId(long wayId) { this.wayId = wayId; }

	public int getDirection() { return direction; }
	public void setDirection(int direction) { this.direction = direction; }

	public void setColor(Color x) { trafficLightColor = x; }
	public Color getColor() { return this.trafficLightColor; }

	// TODO CHECK LOGIC + REFACTOR both setColor() and getColorString()
	public void setColor(String color) {
		if (color.equals("red")) {
			this.trafficLightColor = Color.red;
			return;
		}
		this.trafficLightColor = Color.green;
	}

	public String getColorString() {
		if (this.trafficLightColor == Color.red)
			return "red";
		return "green";
	}

	public void changeColor() {
		if (trafficLightColor == Color.red) {
			setColor(Color.green);
			return;
		}
		/* if (trafficLightColor == Color.yellow) {
			setColor(Color.red);
			return;
		}*/
		if (trafficLightColor == Color.green) {
			setColor(Color.red);
			return;
		}
	}

	public void updateTrafficLightView() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized (mkLock) {
					
					if (currentPoint == null)
						return;
					if (lastMk == null) {
						//lastMk = new MapMarkerTrafficLight(trafficLightColor, currentPoint.lat, currentPoint.lon);
						lastMk = new MapMarkerDot(currentPoint.lat, currentPoint.lon);
						lastMk.setColor(trafficLightColor);
						lastMk.setBackColor(trafficLightColor);
						map.addMapMarker(lastMk);
						return;
					}

					changeColor();
					// remove the traffic light
					if (lastMk != null) {
						map.removeMapMarker(lastMk);
					}
					// add the traffic light again with the new color
					//lastMk = new MapMarkerTrafficLight(trafficLightColor, currentPoint.lat, currentPoint.lon);
					lastMk = new MapMarkerDot(currentPoint.lat, currentPoint.lon);
					lastMk.setColor(trafficLightColor);
					lastMk.setBackColor(trafficLightColor);
					map.addMapMarker(lastMk);
				}

			}
		});

	}
	
	@Override
	public String toString() {
		return this.direction + " " + currentPoint + "\n";
	}
}
