package org.wst;

import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.wst.helper.ClipboardService;
import org.wst.helper.FileManager;
import org.wst.helper.FormatChecker;

public class PrimaryController {

    @FXML
    private TextArea inputArea;
    @FXML
    private VBox colWithListView;
    @FXML
    private VBox colWithSecondListView;

    private ListView<String> fileList;
    private ListView<String> bibList;

    private final FileManager fileManager = FileManager.getInstance();

    public PrimaryController() {

    }

    @FXML
    public void initialize() {
        initListViews();
        initClipboardService();
    }

    @FXML
    private void initClipboardService(){
        ClipboardService service = new ClipboardService();

        // setup service to check clipboard every second
        service.setPeriod(Duration.millis(200));
        service.setOnSucceeded(t -> {
            // get string from clipboard
            if (t.getSource().getValue() != null) {
                String entry = FormatChecker.basicBibTeXCheck((String) t.getSource().getValue());
                System.out.println(entry);
                inputArea.setText(entry.equals("invalid") ? "Not a valid BibTeX entry!" : entry);

                if(!entry.equals("invalid"))App.toFront();

                /* maybe used later
                Window st = textArea1.getScene().getWindow();
                st.requestFocus();
                 */
            } else {
                inputArea.setText("Empty Clipboard!");
            }
        });
        service.start();
    }

    @FXML
    private void initListViews(){
        ObservableList<String> fileNames = FXCollections.observableArrayList("No root selected");
        fileList = new ListView<>(fileNames);
        fileList.setMaxSize(200, 180);

        bibList = new ListView<>();
        bibList.setMaxSize(200, 180);

        colWithListView.getChildren().add(fileList);
        colWithSecondListView.getChildren().add(bibList);

    }
    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

    @FXML
    private void selectRoot(ActionEvent actionEvent) {
        fileManager.selectDirectory(actionEvent, fileList);
    }

    @FXML
    private void selectSingle(ActionEvent actionEvent) {

    }

    @FXML
    private void createFile(ActionEvent actionEvent) {

    }

    @FXML
    private void selectFile(ActionEvent actionEvent) {

    }
}
