
import org.opencv.core.Core;

import controller.Controller;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("view/View.fxml"));
        try {
            BorderPane root = (BorderPane) loader.load();
            root.setStyle("-fx-background-color: whitesmoke;");
            stage.setScene(new Scene(root, 1300, 800));
        } catch (Exception e) {
            e.printStackTrace();
        }
        stage.setTitle("Vehicle counter");
        stage.setResizable(false);
        stage.show();
        Controller controller = loader.getController();
        stage.setOnCloseRequest((new EventHandler<WindowEvent>() {
            public void handle(WindowEvent event) {
                controller.dispose();
            }
        }));
    }

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Application.launch(Main.class, args);
    }

}
