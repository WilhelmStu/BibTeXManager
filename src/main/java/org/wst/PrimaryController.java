package org.wst;

import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.wst.helper.ClipboardService;
import org.wst.helper.FileManager;
import org.wst.helper.FormatChecker;

public class PrimaryController {

    @FXML
    private TextArea inputArea; // replace with textFlow?
    @FXML
    private VBox colWithListView;
    @FXML
    private VBox colWithSecondListView;
    @FXML
    private GridPane gridPane;
    @FXML
    private Label rootDirectory;
    @FXML
    private Label selectedFile;
    @FXML
    private Label secondList;

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
    private void initClipboardService() {
        ClipboardService service = new ClipboardService();

        // setup service to check clipboard every second
        service.setPeriod(Duration.millis(200));
        service.setOnSucceeded(t -> {
            // get string from clipboard
            if (t.getSource().getValue() != null) {
                String entry = FormatChecker.basicBibTeXCheck((String) t.getSource().getValue());
                System.out.println(entry);
                inputArea.setText(entry.equals("invalid") ? "Not a valid BibTeX entry!" : entry);

                if (!entry.equals("invalid")) App.toFront();

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
    private void initListViews() {
        //ObservableList<String> fileNames = FXCollections.observableArrayList("No root selected");
        fileList = new ListView<>();

        bibList = new ListView<>();

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
        this.rootDirectory.setText(fileManager.getRootDirectory());
    }

    @FXML
    private void selectSingleFile(ActionEvent actionEvent) {
        fileManager.selectSingleFile(actionEvent);
        setSelectedFileLabel();
        bibList.setItems(fileManager.populateBibList());
    }


    @FXML
    private void createFile(ActionEvent actionEvent) {
        if (fileManager.createFile(actionEvent)) {
            setSelectedFileLabel();

            bibList.setItems(fileManager.populateBibList());
        }
    }

    @FXML
    private void selectFileFromList(ActionEvent actionEvent) {
        fileManager.selectFileFromList(fileList.getSelectionModel().getSelectedItem());
        setSelectedFileLabel();
        bibList.setItems(fileManager.populateBibList());
    }

    private void setSelectedFileLabel() {
        this.selectedFile.setText(fileManager.getSelectedFileName() + " selected");
        this.selectedFile.setId(fileManager.getSelectedFileName().equals("No file") ?
                "selectedFile" : "selectedFileGreen");
        this.secondList.setText("Entries inside " + fileManager.getSelectedFileName());
    }

    // todo docu!
    public void insertIntoFile(ActionEvent actionEvent) {
        String entry = FormatChecker.basicBibTeXCheck(inputArea.getText());

        if (entry.equals("invalid")) {
            throwAlert("Not a valid bib entry!");

        } else if (!fileManager.isFileSelected()) {
            throwAlert("Select a file first!");

        } else {
            fileManager.writeToFile(entry);
            inputArea.setText("Bib entry successfully inserted into " + fileManager.getSelectedFileName());
        }
    }

    private void throwAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.OK);
        alert.setTitle("Entry not inserted!");
        alert.getDialogPane().getScene().getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        alert.show();
    }
}
