package model;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public final class CVProcessor {
    private long frameTime;
    private Mat avgFrame;
    private List<Point> lastCentroids = new ArrayList<>();
    private List<Point> currentCentroids = new ArrayList<>();
    private List<Vehicle> vehicles = new ArrayList<>();
    private int vehicleCounter = 0;

    public void setFrameTime() {
        frameTime = System.currentTimeMillis();
    }

    public Mat getDifferenceFrame(Mat frame, double avgWeight) {
        Mat grayFrame = new Mat();
        Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);

        Mat grayBlurredFrame = new Mat();
        Imgproc.GaussianBlur(grayFrame, grayBlurredFrame, new Size(21, 21), 0);

        grayBlurredFrame.convertTo(grayBlurredFrame, CvType.CV_32F);
        if (avgFrame == null) {
            avgFrame = grayBlurredFrame.clone();
        }
        Imgproc.accumulateWeighted(grayBlurredFrame, avgFrame, avgWeight);

        Mat differenceFrame = new Mat();
        Core.absdiff(avgFrame, grayBlurredFrame, differenceFrame);
        differenceFrame.convertTo(differenceFrame, CvType.CV_8UC3);
        return differenceFrame;
    }

    public Mat getThresholdFrame(Mat differenceFrame, double threshold) {
        Mat thresholdFrame = new Mat();
        Imgproc.threshold(differenceFrame, thresholdFrame, threshold, 255, Imgproc.THRESH_BINARY);

        Mat dilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Mat erode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.erode(thresholdFrame, thresholdFrame, erode);
        Imgproc.erode(thresholdFrame, thresholdFrame, erode);
        Imgproc.dilate(thresholdFrame, thresholdFrame, dilate);
        Imgproc.dilate(thresholdFrame, thresholdFrame, dilate);
        return thresholdFrame;
    }

    public List<MatOfPoint> getContours(Mat thresholdFrame, double contourSize, double xDist, double yDist) {
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(thresholdFrame, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        List<MatOfPoint> contoursToRemove = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            if (Imgproc.contourArea(contour) < contourSize) {
                contoursToRemove.add(contour);
            }
        }
        contours.removeAll(contoursToRemove);

        List<MatOfPoint2f> contours2f = new ArrayList<>();
        for (MatOfPoint contour : contours) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32FC2);
            Imgproc.approxPolyDP(contour2f, contour2f, 0.05 * Imgproc.arcLength(contour2f, true), true);
            contours2f.add(contour2f);
        }
        contours.clear();
        for (MatOfPoint2f contour2f : contours2f) {
            MatOfPoint contour = new MatOfPoint();
            contour2f.convertTo(contour, CvType.CV_32SC2);
            contours.add(contour);
        }

        if (!contours.isEmpty()) {
            contours = auxMergeContours(contours, contours.size() - 1, xDist, yDist);
        }
        return contours;
    }

    private List<MatOfPoint> auxMergeContours(List<MatOfPoint> contours, int iterations, double xD, double yD) {
        int count = 0;
        while (count < iterations) {
            List<MatOfPoint> clipBoard = new ArrayList<>();
            boolean[] isAdded = new boolean[contours.size()];
            for (int i = 0; i < contours.size() - 1; i++) {
                if (auxClose(contours.get(i), contours.get(i + 1), xD, yD)) {
                    MatOfPoint mergedContour = new MatOfPoint();
                    List<Point> points = new ArrayList<>();
                    points.addAll(contours.get(i).toList());
                    points.addAll(contours.get(i + 1).toList());
                    mergedContour.fromList(points);

                    MatOfInt indices = new MatOfInt();
                    Imgproc.convexHull(mergedContour, indices, false);
                    points.clear();
                    for (int index : indices.toList()) {
                        points.add(mergedContour.toList().get(index));
                    }
                    mergedContour.fromList(points);

                    clipBoard.add(mergedContour);
                    isAdded[i] = true;
                } else {
                    if (!isAdded[i]) {
                        clipBoard.add(contours.get(i));
                        isAdded[i] = true;
                    }
                    if (i == contours.size() - 2) {
                        clipBoard.add(contours.get(contours.size() - 1));
                    }
                }
            }
            contours = new ArrayList<>(clipBoard);
            count++;
        }
        return contours;
    }

    private boolean auxClose(MatOfPoint contour1, MatOfPoint contour2, double xDist, double yDist) {
        for (Point point1 : contour1.toArray()) {
            for (Point point2 : contour2.toArray()) {
                if (Math.abs(point1.x - point2.x) < xDist && Math.abs(point1.y - point2.y) < yDist) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Point> getCurrentCentroids(List<MatOfPoint> contours) {
        if (!contours.isEmpty()) {
            lastCentroids = new ArrayList<>(currentCentroids);
            currentCentroids.clear();
            for (MatOfPoint contour : contours) {
                Rect rect = Imgproc.boundingRect(contour);
                Point centroid = new Point((int) rect.x + rect.width / 2, (int) rect.y + rect.height / 2);
                currentCentroids.add(centroid);
            }
        } else {
            lastCentroids.clear();
            currentCentroids.clear();
        }
        return currentCentroids;
    }

    public void addCentroidsToVehicle(double lockonDistance) {
        List<Point> candidates = new ArrayList<>(currentCentroids);
        if (!vehicles.isEmpty()) {
            for (Vehicle vehicle : vehicles) {
                for (Point centroid : candidates) {
                    double distance = auxGetDistance(centroid, vehicle.getLastPoint());
                    if (distance < lockonDistance) {
                        if ((vehicle.getDirection().equals("left") && vehicle.getLastPoint().x > centroid.x)
                                || (vehicle.getDirection().equals("right") && vehicle.getLastPoint().x < centroid.x)) {
                            vehicle.addPoint(centroid);
                            vehicle.setLastSeen(frameTime);
                            candidates.remove(centroid);
                            break;
                        }
                    }
                }
            }
            if (!candidates.isEmpty()) {
                for (Point centroid : candidates) {
                    addNewVehicle(centroid, lockonDistance);
                }
            }
        } else {
            for (Point centroid : candidates) {
                addNewVehicle(centroid, lockonDistance);
            }
        }
    }

    private void addNewVehicle(Point current, double lockonDistance) {
        for (Point last : lastCentroids) {
            double distance = auxGetDistance(current, last);
            if (distance < lockonDistance) {
                Vehicle vehicle = new Vehicle(frameTime);
                vehicle.addPoint(last);
                vehicle.addPoint(current);
                if (current.x < last.x) {
                    vehicle.setDirection("left");
                } else {
                    vehicle.setDirection("right");
                }
                vehicles.add(vehicle);
            }
        }
    }

    private double auxGetDistance(Point point1, Point point2) {
        double x = Math.pow(point1.x - point2.x, 2);
        double y = Math.pow(point1.y - point2.y, 2);
        return Math.sqrt(x + y);
    }

    public void removeExpiredVehicles(double vehicleTimeout, boolean saveData) {
        List<Vehicle> vehiclesToRemove = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            if ((frameTime - vehicle.getLastSeen()) / 1000 > vehicleTimeout) {
                if (saveData && vehicle.isFound()) {
                    Writer.write(vehicle);
                }
                vehiclesToRemove.add(vehicle);
            }
        }
        vehicles.removeAll(vehiclesToRemove);
    }

    public void detectVehicles(double centerX) {
        for (Vehicle vehicle : vehicles) {
            if (!vehicle.isFound()) {
                int startX = 0;
                int endX = 0;
                int firstX = (int) vehicle.getFirstPoint().x;
                int lastX = (int) vehicle.getLastPoint().x;
                if (firstX < lastX) {
                    startX = firstX;
                    endX = lastX;
                } else {
                    startX = lastX;
                    endX = firstX;
                }
                for (int i = startX; i < endX; i++) {
                    if (i == (int) centerX) {
                        vehicleCounter++;
                        vehicle.setFound(true);
                    }
                }
            }
        }
    }

    public void detectSpeed(double rightBarrier, double leftBarrier, double distance) {
        for (Vehicle vehicle : vehicles) {
            if (vehicle.getSpeed() == 0.0) {
                if (vehicle.getRightBarrier() != 0 && vehicle.getLeftBarrier() != 0) {
                    double time = Math.abs(vehicle.getRightBarrier() - vehicle.getLeftBarrier());
                    double speed = Math.round(((distance * 3600000 / time) / 1000) * 100.0) / 100.0;
                    vehicle.setSpeed(speed);
                } else {
                    int startX = 0;
                    int endX = 0;
                    int firstX = (int) vehicle.getFirstPoint().x;
                    int lastX = (int) vehicle.getLastPoint().x;
                    if (firstX < lastX) {
                        startX = firstX;
                        endX = lastX;
                    } else {
                        startX = lastX;
                        endX = firstX;
                    }
                    for (int i = startX; i < endX; i++) {
                        if (vehicle.getRightBarrier() == 0 && i == (int) rightBarrier) {
                            vehicle.setRightBarrier(frameTime);
                        }
                        if (vehicle.getLeftBarrier() == 0 && i == (int) leftBarrier) {
                            vehicle.setLeftBarrier(frameTime);
                        }
                    }
                }
            }
        }
    }

    public Mat getAvgFrame() {
        Mat avgFrame8UC3 = new Mat();
        avgFrame.convertTo(avgFrame8UC3, CvType.CV_8UC3);
        return avgFrame8UC3;
    }

    public void clearAvgFrame() {
        avgFrame = null;
    }

    public List<Point> getLastCentroids() {
        return lastCentroids;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public int getVehicleCounter() {
        return vehicleCounter;
    }

    public void resetVehicles() {
        vehicles.clear();
    }

    public void resetVehicleCounter() {
        vehicleCounter = 0;
    }
}
