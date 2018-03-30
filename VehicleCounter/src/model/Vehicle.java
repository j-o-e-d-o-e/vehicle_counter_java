package model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.opencv.core.Point;

public class Vehicle {
	private String id;
	private long firstSeen;
	private long lastSeen;
	private long leftBarrier;
	private long rightBarrier;
	private double speed;
	private String direction;
	private boolean found;

	private List<Point> track = new ArrayList<>();

	public Vehicle(long firstSeen) {
		this.id = UUID.randomUUID().toString().substring(0, 8);
		this.firstSeen = firstSeen;
		this.lastSeen = firstSeen;
	}

	public String getId() {
		return id;
	}

	public long getFirstSeen() {
		return this.firstSeen;
	}

	public void setLastSeen(long lastSeen) {
		this.lastSeen = lastSeen;
	}

	public long getLastSeen() {
		return this.lastSeen;
	}

	public long getLeftBarrier() {
		return leftBarrier;
	}

	public void setLeftBarrier(long leftBarrier) {
		this.leftBarrier = leftBarrier;
	}

	public long getRightBarrier() {
		return rightBarrier;
	}

	public void setRightBarrier(long rightBarrier) {
		this.rightBarrier = rightBarrier;
	}

	public double getSpeed() {
		return speed;
	}

	public void setSpeed(double speed) {
		this.speed = speed;
	}

	public String getDirection() {
		return this.direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public boolean isFound() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}

	public List<Point> getTrack() {
		return this.track;
	}
	
	public Point getFirstPoint() {
		return this.track.get(0);
	}

	public Point getLastPoint() {
		return this.track.get(track.size() - 1);
	}

	public void addPoint(Point point) {
		this.track.add(point);
	}

}
