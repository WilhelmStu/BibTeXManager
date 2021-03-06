package org.wst;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static Stage stage1;
    private static App instance;
    private static PrimaryController controller;

    @Override
    public void start(Stage stage) throws IOException {
        instance = this;
        scene = new Scene(loadFXML("primary"), 1024, 768);
        scene.getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        scene.getStylesheets().add(App.class.getResource("darkStyles.css").toExternalForm());
        stage.setTitle("BibTeX Manager");
        stage.getIcons().add(new Image(App.class.getResource("icons/icon.png").toExternalForm()));
        stage.setScene(scene);
        stage.setMinHeight(480);
        stage.setMinWidth(640);
        stage.show();
        stage.setOnCloseRequest(t -> {
            System.out.println("Closing app and all background Threads!");
            Platform.exit();
            System.exit(0);
        });
        stage1 = stage;
        controller.setStageAndListeners(stage);
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        Parent p = fxmlLoader.load();
        controller = fxmlLoader.getController();
        return p;
    }

    /**
     * Will put the app in front of all other windows
     */
    public static void toFront() {
        if (stage1.isIconified()) stage1.setIconified(false);
        stage1.requestFocus();
        stage1.setAlwaysOnTop(true);
        stage1.setAlwaysOnTop(false);
        stage1.toFront();
    }

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        launch();
    }
}