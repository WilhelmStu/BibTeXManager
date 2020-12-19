package org.wst;

import java.io.IOException;
import java.util.Collections;

import javafx.collections.ObservableList;
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


    /**
     * This is called before the scene is displayed, but after the Classes Constructor
     */
    @FXML
    public void initialize() {
        initListViews();
        initClipboardService();
    }

    /**
     * Will start the ClipboardService that is run every 200ms.
     * Every iteration of the service will run until a valid Bib-Entry is found in the
     * System clipboard.
     * The TextField will be changed accordingly and if the entry is valid the
     * program window will come to the front of the OS
     */
    @FXML
    private void initClipboardService() {
        ClipboardService service = new ClipboardService();
        inputArea.setText("Empty Clipboard!");

        // setup service to check clipboard every second
        service.setPeriod(Duration.millis(200));
        service.setOnSucceeded(t -> {
            // get string from clipboard
            if (t.getSource().getValue() != null) {
                String entry = FormatChecker.basicBibTeXCheck((String) t.getSource().getValue());
                boolean valid = (!entry.equals("invalid"));
                if (valid) {
                    entry = FormatChecker.replaceQuotationMarks(entry);
                    System.out.println(entry);
                    inputArea.setText(entry);
                    App.toFront();
                } else {
                    inputArea.setText("Not a valid BibTeX entry!");
                }

                /* maybe used later
                Window st = textArea1.getScene().getWindow();
                st.requestFocus();
                 */
            }
        });
        service.start();
    }

    /**
     * Creates both ListViews of the App and inserts them at the appropriate places in the UI
     */
    @FXML
    private void initListViews() {
        //ObservableList<String> fileNames = FXCollections.observableArrayList("No root selected");
        fileList = new ListView<>();

        bibList = new ListView<>();

        colWithListView.getChildren().add(fileList);
        colWithSecondListView.getChildren().add(1, bibList);

    }

    /**
     * Currently unused, might be needed later for a second scene
     *
     * @throws IOException
     */
    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

    /**
     * Will call FileManager to search for bib files and change the
     * label to the selected directory
     *
     * @param actionEvent click button
     */
    @FXML
    private void selectRoot(ActionEvent actionEvent) {
        fileManager.selectDirectory(actionEvent, fileList);
        this.rootDirectory.setText(fileManager.getRootDirectory());
    }

    /**
     * Will call FileManager to let the user select a single file from file system
     * and fill the bibList with the entries in the file
     *
     * @param actionEvent click button
     */
    @FXML
    private void selectSingleFile(ActionEvent actionEvent) {
        fileManager.selectSingleFile(actionEvent);
        setSelectedFileLabel();
        fileManager.populateBibList(bibList);
    }

    /**
     * Will call FileManager to let user save a new file on the file system and
     * change the bibList accordingly
     *
     * @param actionEvent click button
     */
    @FXML
    private void createFile(ActionEvent actionEvent) {
        if (fileManager.createFile(actionEvent)) {
            setSelectedFileLabel();

            fileManager.populateBibList(bibList);
        }
    }


    /**
     * Will call FileManager to set the selected file and update the bibList
     *
     * @param actionEvent click button
     */
    @FXML
    private void selectFileFromList(ActionEvent actionEvent) {
        fileManager.selectFileFromList(fileList.getSelectionModel().getSelectedItem());
        setSelectedFileLabel();
        fileManager.populateBibList(bibList);
    }

    /**
     * Called whenever the label of the selected file changes. It will be red if no file
     * is selected and green otherwise.
     * Also the Label of the bibList is set appropriately
     */
    private void setSelectedFileLabel() {
        this.selectedFile.setText(fileManager.getSelectedFileName() + " selected");
        this.selectedFile.setId(fileManager.getSelectedFileName().equals("No file") ?
                "selectedFile" : "selectedFileGreen");
        this.secondList.setText("Entries inside " + fileManager.getSelectedFileName());
    }

    /**
     * Will check if the current entry inside the TextArea is valid and insert the
     * valid entry with the fileManager
     * If the entry is not valid or there is no file selected an alert box will be opened
     *
     * @param actionEvent click button
     */
    public void insertIntoFile(ActionEvent actionEvent) {
        String entry = FormatChecker.basicBibTeXCheck(inputArea.getText());

        if (entry.equals("invalid")) {
            throwAlert("Not a valid bib entry!");

        } else if (!fileManager.isFileSelected()) {
            throwAlert("Select a file first!");

        } else {
            fileManager.writeToFile(entry);
            ObservableList<String> entries = bibList.getItems();
            String keyword = FormatChecker.getBibEntryKeyword(entry);
            if (keyword != null) {
                if (!entries.contains(keyword)) entries.add(keyword);
            }
            Collections.sort(entries);
            inputArea.setText("Bib entry successfully inserted into " + fileManager.getSelectedFileName());
        }
    }

    /**
     * Displays an alert box that can be dismissed with an OK-button
     *
     * @param msg msg to display inside the alert box
     */
    private void throwAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.OK);
        alert.setTitle("Entry not inserted!");
        alert.getDialogPane().getScene().getStylesheets().add(App.class.getResource("styles.css").toExternalForm());
        alert.show();
    }

    /**
     * Will put the selected item into the TextArea for editing
     *
     * @param actionEvent click button
     */
    public void selectEntry(ActionEvent actionEvent) {
        if (fileManager.isFileSelected() && bibList.getSelectionModel().getSelectedItem() != null) {
            inputArea.setText(FormatChecker.replaceQuotationMarks(
                    fileManager.getBibEntry(bibList.getSelectionModel().getSelectedItem())));
        } else {
            throwAlert("Select a file first!");
        }
    }
}
