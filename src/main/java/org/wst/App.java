package org.wst;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static Stage stage1;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("primary"), 640, 480);
        stage.setTitle("BibTeX Manager");
        stage.setScene(scene);
        stage.show();
        stage1 = stage;
    }

    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    /**
     * Will put the app in front of all other windows
     */
    public static void toFront(){
        if(stage1.isIconified()) stage1.setIconified(false);
        stage1.requestFocus();
        stage1.setAlwaysOnTop(true);
        stage1.setAlwaysOnTop(false);
        stage1.toFront();
    }
    public static void main(String[] args) {
        launch();
    }

}