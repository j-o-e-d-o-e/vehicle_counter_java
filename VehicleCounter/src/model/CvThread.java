package model;

public class CvThread implements Runnable {

    @Override
    public void run() {
        // Mat frame = new Mat();
        // if (capture.isOpened()) {
        // try {
        // capture.read(frame);
        // if (!frame.empty()) {
        // cv.setFrameTime();
        // Mat differenceFrame = cv.getDifferenceFrame(frame, avgWeight.getValue());
        // Mat thresholdFrame = cv.getThresholdFrame(differenceFrame,
        // threshold.getValue());
        // List<MatOfPoint> contours = cv.getContours(thresholdFrame,
        // contourSize.getValue(),
        // contourXDist.getValue(), contourYDist.getValue());
        // List<Point> currentCentroids = cv.getCurrentCentroids(contours);
        // List<Point> lastCentroids = cv.getLastCentroids();
        // if (!lastCentroids.isEmpty() && !currentCentroids.isEmpty()) {
        // cv.addCentroidsToVehicle(lockonDist.getValue());
        // }
        // if (!cv.getVehicles().isEmpty()) {
        // cv.removeExpiredVehicles(vehicleTimeout.getValue(),
        // saveCheckBox.isSelected());
        // cv.detectVehicles(centerX.getValue());
        // }
        // if (speedCheckBox.isSelected()) {
        // speedLabel.setVisible(true);
        // cv.detectSpeed(rightBarrier.getValue(), leftBarrier.getValue(),
        // speedDistance.getValue());
        // } else {
        // speedLabel.setVisible(false);
        // }
        // updateText();
        // draw(frame, contours, currentCentroids);
        // Utils.onFXThread(differenceImage.imageProperty(),
        // Utils.mat2Image(differenceFrame));
        // Utils.onFXThread(processedImage.imageProperty(),
        // Utils.mat2Image(thresholdFrame));
        // Utils.onFXThread(originalImage.imageProperty(), Utils.mat2Image(frame));
        // }
        // } catch (Exception e) {
        // Utils.onFXThread(messageCounter, "Failed to process image... ");
        // }
        // }
    }

}
