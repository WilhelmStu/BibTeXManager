package org.wst;

import java.io.IOException;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.wst.helper.ClipboardService;
import org.wst.helper.FileManager;
import org.wst.helper.FormatChecker;
import org.wst.model.TableEntry;

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
    @FXML
    private TableView<TableEntry> bibTable;
    @FXML
    private ListView<String> fileList;
    @FXML
    private HBox buttonBox;

    private final FileManager fileManager = FileManager.getInstance();

    public PrimaryController() {

    }


    /**
     * This is called before the scene is displayed, but after the Classes Constructor
     */
    @FXML
    public void initialize() {
        initListAndTable();
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
            }
        });
        service.start();
    }

    // todo config for more data in table

    /**
     * Creates both ListViews of the App and inserts them at the appropriate places in the UI
     */
    @FXML
    private void initListAndTable() {
        //ObservableList<String> fileNames = FXCollections.observableArrayList("No root selected");

        TableColumn<TableEntry, String> keyColumn = new TableColumn<>("Key");
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("keyword"));

        TableColumn<TableEntry, String> authorColumn = new TableColumn<>("Author/s");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));

        TableColumn<TableEntry, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<TableEntry, String> yearColumn = new TableColumn<>("Year");
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));

        bibTable.getColumns().add(keyColumn);
        bibTable.getColumns().add(authorColumn);
        bibTable.getColumns().add(titleColumn);
        bibTable.getColumns().add(yearColumn);
        bibTable.setPlaceholder(new Label("No Data to display yet!"));

        bibTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        /* maybe used later
        keyColumn.prefWidthProperty().bind(bibTable.widthProperty().multiply(0.2));
        authorColumn.prefWidthProperty().bind(bibTable.widthProperty().multiply(0.35));
        titleColumn.prefWidthProperty().bind(bibTable.widthProperty().multiply(0.35));
        yearColumn.prefWidthProperty().bind(bibTable.widthProperty().multiply(0.1));
         */

        yearColumn.setMinWidth(60);
        yearColumn.setMaxWidth(60);
        yearColumn.setResizable(false);

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
        fileManager.readFileIntoTable(bibTable);
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
            fileManager.readFileIntoTable(bibTable);
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
        fileManager.readFileIntoTable(bibTable);
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
            ObservableList<TableEntry> entries = bibTable.getItems();
            String keyword = FormatChecker.getBibEntryKeyword(entry);
            if (keyword != null) {
                boolean isAlreadyInFile = false;
                for (int i = 0; i < entries.size(); i++) {
                    if (entries.get(i).getKeyword().equals(keyword)) {
                        entries.set(i, FormatChecker.getBibTableEntry(entry));
                        isAlreadyInFile = true;
                        break;
                    }
                }
                if (!isAlreadyInFile) {
                    entries.add(FormatChecker.getBibTableEntry(entry));
                }

            }
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
        if (fileManager.isFileSelected() && bibTable.getSelectionModel().getSelectedItem() != null) {
            inputArea.setText(FormatChecker.replaceQuotationMarks(
                    fileManager.getBibEntry(bibTable.getSelectionModel().getSelectedItem())));
        } else {
            throwAlert("Select a file first!");
        }
    }
}
