package controller;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.CVProcessor;
import model.Utils;
import model.Vehicle;
import model.Writer;

public class Controller {
    @FXML
    private BorderPane root;
    @FXML
    private MenuItem closeMenu;
    @FXML
    private ToggleGroup toggleDevice;
    @FXML
    private RadioMenuItem firstCamera;
    @FXML
    private RadioMenuItem secondCamera;
    @FXML
    private RadioMenuItem drawContours;
    @FXML
    private RadioMenuItem drawVehicles;
    @FXML
    private RadioMenuItem drawVehicleLabels;
    @FXML
    private MenuItem loadValues;
    @FXML
    private MenuItem saveValues;
    @FXML
    private MenuItem saveData;
    @FXML
    private Label originalImageLabel;
    @FXML
    private Label differenceImageLabel;
    @FXML
    private Label thresholdImageLabel;
    @FXML
    private ImageView originalImage;
    @FXML
    private ImageView differenceImage;
    @FXML
    private ImageView processedImage;
    @FXML
    private Slider avgWeight;
    @FXML
    private Slider threshold;
    @FXML
    private Slider contourSize;
    @FXML
    private Slider contourXDist;
    @FXML
    private Slider contourYDist;
    @FXML
    private Slider lockonDist;
    @FXML
    private Slider vehicleTimeout;
    @FXML
    private Slider centerX;
    @FXML
    private Slider rightBarrier;
    @FXML
    private Slider leftBarrier;
    @FXML
    private Slider speedDistance;
    @FXML
    private Button defaultButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button cameraButton;
    @FXML
    private Button resetButton;
    @FXML
    private CheckBox speedCheckBox;
    @FXML
    private CheckBox saveCheckBox;
    @FXML
    private Label countLabel;
    @FXML
    private Label speedLabel;
    @FXML
    private Label valuesLabel;

    private List<Slider> sliders;
    private FileChooser fileChooser;
    private double[] defaultUserValues;
    private double currentSpeed;
    private ObjectProperty<String> messageCounter;
    private ObjectProperty<String> messageSpeed;
    private ObjectProperty<String> messageValues;
    private CVProcessor cv;
    private VideoCapture capture;
    private ScheduledExecutorService timer;
    private boolean stopped;

    public Controller() {
        cv = new CVProcessor();
        capture = new VideoCapture();
        stopped = true;
        sliders = new ArrayList<>();
        fileChooser = new FileChooser();
        defaultUserValues = new double[11];
        messageCounter = new SimpleObjectProperty<>();
        messageSpeed = new SimpleObjectProperty<>();
        messageValues = new SimpleObjectProperty<>();
    }

    @FXML
    private void initialize() {
        createSlidersArray();
        getDefaultValuesFromSliders();
        countLabel.textProperty().bind(messageCounter);
        speedLabel.textProperty().bind(messageSpeed);
        valuesLabel.textProperty().bind(messageValues);
        setWidth(originalImage, 735);
        setWidth(differenceImage, 350);
        setWidth(processedImage, 350);
    }

    @FXML
    private void startCamera() {
        originalImageLabel.setVisible(true);
        differenceImageLabel.setVisible(true);
        thresholdImageLabel.setVisible(true);
        cv.clearAvgFrame();
        if (stopped) {
            int deviceNum = Integer.parseInt(((RadioMenuItem) toggleDevice.getSelectedToggle()).getId());
            capture.open(deviceNum);
            if (capture.isOpened()) {
                stopped = false;
                Runnable thread = new Runnable() {
                    @Override
                    public void run() {
                        onCVThread();
                    }
                };
                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(thread, 0, 33, TimeUnit.MILLISECONDS);
                cameraButton.setText("Stop camera");
            } else {
                Utils.onFXThread(messageCounter, "Failed to connect the camera...");
            }
        } else {
            stopped = true;
            cameraButton.setText("Start camera");
            stopCamera();
        }
    }

    private void onCVThread() {
        Mat frame = new Mat();
        if (capture.isOpened()) {
            try {
                capture.read(frame);
                if (!frame.empty()) {
                    cv.setFrameTime();
                    Mat differenceFrame = cv.getDifferenceFrame(frame, avgWeight.getValue());
                    Mat thresholdFrame = cv.getThresholdFrame(differenceFrame, threshold.getValue());
                    List<MatOfPoint> contours = cv.getContours(thresholdFrame, contourSize.getValue(),
                            contourXDist.getValue(), contourYDist.getValue());
                    List<Point> currentCentroids = cv.getCurrentCentroids(contours);
                    List<Point> lastCentroids = cv.getLastCentroids();
                    if (!lastCentroids.isEmpty() && !currentCentroids.isEmpty()) {
                        cv.addCentroidsToVehicle(lockonDist.getValue());
                    }
                    if (!cv.getVehicles().isEmpty()) {
                        cv.removeExpiredVehicles(vehicleTimeout.getValue(), saveCheckBox.isSelected());
                        cv.detectVehicles(centerX.getValue());
                    }
                    if (speedCheckBox.isSelected()) {
                        speedLabel.setVisible(true);
                        cv.detectSpeed(rightBarrier.getValue(), leftBarrier.getValue(), speedDistance.getValue());
                    } else {
                        speedLabel.setVisible(false);
                    }
                    updateText();
                    draw(frame, contours, currentCentroids);
                    Utils.onFXThread(differenceImage.imageProperty(), Utils.mat2Image(differenceFrame));
                    Utils.onFXThread(processedImage.imageProperty(), Utils.mat2Image(thresholdFrame));
                    Utils.onFXThread(originalImage.imageProperty(), Utils.mat2Image(frame));
                }
            } catch (Exception e) {
                Utils.onFXThread(messageCounter, "Failed to process image... ");
            }
        }
    }

    private void updateText() {
        int currentVehicles = 0;
        for (Vehicle vehicle : cv.getVehicles()) {
            if (vehicle.getTrack().size() > 3) {
                currentVehicles++;
            }
        }
        String count = "Vehicles counted: " + cv.getVehicleCounter() + "\t\tVehicles currently tracked: "
                + currentVehicles;
        Utils.onFXThread(this.messageCounter, count);
        String currentValues = "";
        for (Slider slider : sliders) {
            if (!slider.getId().equals("centerX") && !slider.getId().equals("rightBarrier")
                    && !slider.getId().equals("leftBarrier") && !slider.getId().equals("speedDistance")) {
                currentValues = currentValues + slider.getId() + ": " + String.format("%.2f", slider.getValue())
                        + "\t\t";
            }
        }
        if (speedCheckBox.isSelected()) {
            currentValues = currentValues + "\tSpeed distance: " + String.format("%.2f", speedDistance.getValue());
            for (Vehicle vehicle : cv.getVehicles()) {
                if (vehicle.getSpeed() != 0.0) {
                    currentSpeed = vehicle.getSpeed();
                    break;
                }
            }
            String speed = "Speed detection: ";
            if (currentSpeed != 0.0) {
                speed = speed + String.format("%.2f", currentSpeed) + " km/h   ";
            }
            Utils.onFXThread(this.messageSpeed, speed);
        }
        Utils.onFXThread(messageValues, currentValues);
    }

    private void draw(Mat frame, List<MatOfPoint> contours, List<Point> centroids) {
        Scalar RED = new Scalar(0, 0, 255);
        Scalar GREEN = new Scalar(0, 255, 0);
        Scalar BLUE = new Scalar(255, 0, 0);
        Scalar YELLOW = new Scalar(0, 255, 255);
        Scalar WHITE = new Scalar(255, 255, 255);
        Imgproc.line(frame, new Point(10, 20), new Point(110, 20), WHITE);
        Imgproc.putText(frame, "0", new Point(10, 20), Core.FONT_HERSHEY_SIMPLEX, 0.5, WHITE);
        Imgproc.putText(frame, "100", new Point(110, 20), Core.FONT_HERSHEY_SIMPLEX, 0.5, WHITE);
        double y = originalImage.getFitWidth() + 50;
        Imgproc.line(frame, new Point(centerX.getValue(), 0), new Point(centerX.getValue(), y), RED, 2);
        if (speedCheckBox.isSelected()) {
            Imgproc.line(frame, new Point(rightBarrier.getValue(), 0), new Point(rightBarrier.getValue(), y), YELLOW,
                    2);
            Imgproc.line(frame, new Point(leftBarrier.getValue(), 0), new Point(leftBarrier.getValue(), y), YELLOW, 2);
        }
        if (drawContours.isSelected()) {
            Imgproc.drawContours(frame, contours, -1, GREEN, 2);
            for (Point centroid : centroids) {
                Imgproc.circle(frame, centroid, 10, RED, -1);
            }
        }
        if (drawVehicles.isSelected()) {
            for (Vehicle vehicle : cv.getVehicles()) {
                if (vehicle.getTrack().size() > 3) {
                    Imgproc.line(frame, vehicle.getFirstPoint(), vehicle.getLastPoint(), RED);
                    for (Point point : vehicle.getTrack()) {
                        Imgproc.circle(frame, point, 5, BLUE, -1);
                    }
                    if (drawVehicleLabels.isSelected()) {
                        Imgproc.putText(frame, vehicle.getId().substring(0, 4), vehicle.getFirstPoint(),
                                Core.FONT_HERSHEY_SIMPLEX, 0.7, WHITE, 1);
                    }
                }
            }
        }
    }

    @FXML
    private void setSlidersToDefaultValues() {
        for (int i = 0; i < sliders.size(); i++) {
            sliders.get(i).setValue(defaultUserValues[i]);
        }
    }

    @FXML
    private void loadUserValues() {
        fileChooser.setTitle("Load values from...");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showOpenDialog(cameraButton.getScene().getWindow());
        if (file != null) {
            BufferedReader reader;
            try {
                reader = new BufferedReader(new FileReader(file.toString()));
                for (int i = 0; i < defaultUserValues.length - 1; i++) {
                    defaultUserValues[i] = Double.parseDouble(reader.readLine());
                }
                reader.close();
            } catch (IOException e) {
                e.getMessage();
                e.printStackTrace();
            }
            setSlidersToDefaultValues();
        }
    }

    @FXML
    private void saveUserValues() {
        fileChooser.setTitle("Save values to...");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(cameraButton.getScene().getWindow());
        if (file != null) {
            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(file.toString()));
                for (Slider slider : sliders) {
                    writer.write(Double.toString(slider.getValue()) + "\n");
                }
                writer.close();
            } catch (IOException e) {
                e.getMessage();
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void setSaveDataPath() {
        fileChooser.setTitle("Save data to...");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(cameraButton.getScene().getWindow());
        if (file != null) {
            Writer.setPath(file.toString());
        }
    }

    @FXML
    private void stopCamera() {
        if (timer != null && !timer.isShutdown()) {
            try {
                timer.shutdown();
                timer.awaitTermination(33, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Utils.onFXThread(messageCounter, "Failed to stop frame processing. Releasing camera now...");
            }
        }
        if (capture.isOpened()) {
            capture.release();
        }
    }

    @FXML
    private void resetCounter() {
        cv.resetVehicleCounter();
        cv.resetVehicles();
        currentSpeed = 0.0;
    }

    public void createSlidersArray() {
        sliders.clear();
        sliders.add(avgWeight);
        sliders.add(threshold);
        sliders.add(contourSize);
        sliders.add(contourXDist);
        sliders.add(contourYDist);
        sliders.add(lockonDist);
        sliders.add(vehicleTimeout);
        sliders.add(centerX);
        sliders.add(rightBarrier);
        sliders.add(leftBarrier);
        sliders.add(speedDistance);
    }

    public void getDefaultValuesFromSliders() {
        for (int i = 0; i < sliders.size(); i++) {
            defaultUserValues[i] = sliders.get(i).getValue();
        }
    }

    private void setWidth(ImageView image, int dimension) {
        image.setFitWidth(dimension);
        image.setPreserveRatio(true);
    }

    @FXML
    public void dispose() {
        if (Writer.isOpen()) {
            Writer.close();
        }
        stopCamera();
        Stage stage = (Stage) cameraButton.getScene().getWindow();
        stage.close();
    }
}
