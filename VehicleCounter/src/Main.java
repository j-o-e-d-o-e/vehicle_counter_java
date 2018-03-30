

import org.opencv.core.Core;

import controller.VehicleCounter;
import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Application.launch(VehicleCounter.class, args);
    }
}
