package org.wst;

import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.wst.helper.*;
import org.wst.model.TableEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


public class PrimaryController {


    @FXML
    private TextArea inputArea; // replace with textFlow?
    @FXML
    private Label rootDirectory, secondList;
    @FXML
    private TableView<TableEntry> bibTable;
    @FXML
    private ListView<String> fileList;
    @FXML
    private HBox buttonBox2;
    @FXML
    private VBox buttonBox3, colWithListView, secondColumnBox;
    @FXML
    private Button insertButton, selectFile, enableFileListButton, undoMoveButton, redoMoveButton,
            toggleAutoInsert, changeClosureMode;
    @FXML
    private ColumnConstraints listViewColumnConstraints, secondColumnConstraints;
    @FXML
    private Region secondColumnRegion;

    private boolean isBelowSize = false;
    private double height = 768;
    private double width = 1024;
    private final ObservableList<Node> buttons = FXCollections.observableArrayList();
    private Stage stage;
    public static boolean isDarkMode = true;
    private static boolean toCurlyMode = true;
    private boolean isFileListActive = true;
    private final FileManager fileManager = FileManager.getInstance();
    private UndoRedoManager undoRedo = UndoRedoManager.getInstance();
    private ClipboardService clipboardService;

    public PrimaryController() {
    }


    /**
     * This is called before the scene is displayed, but after the Classes Constructor
     */
    @FXML
    public void initialize() {
        initClipboardService();
        initListAndTable();
        initShortCutListener();
        fileManager.setUndoRedoButtons(undoMoveButton, redoMoveButton);
        getAllButtons();
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
        this.clipboardService = new ClipboardService();
        inputArea.setText("Empty Clipboard!");

        // setup service to check clipboard every second
        clipboardService.setPeriod(Duration.millis(200));
        clipboardService.setOnSucceeded(t -> {
            // get string from clipboard
            if (t.getSource().getValue() != null) {
                String entry = FormatChecker.basicBibTeXCheck((String) t.getSource().getValue());
                boolean valid = (!entry.isEmpty());
                if (valid) {
                    entry = FormatChecker.replaceValueClosures(entry, toCurlyMode);
                    System.out.println(entry);

                    String currentText = inputArea.getText();
                    if (currentText.length() < 30) {
                        inputArea.setText(entry);
                    } else if (!inputArea.getText().contains(entry.replaceAll("\r", "").trim())) {
                        inputArea.setText(inputArea.getText() + "\n\n" + entry);
                    }
                    App.toFront();
                } else {
                    // do nothing for now, since user might copy to change previous entry
                    //inputArea.setText("Not a valid BibTeX entry! (from Clipboard)");
                }
            }
        });
        clipboardService.start();
    }

    // todo config for more data in table

    /**
     * Creates the layout and columns of the tableview, also defines custom
     * table cells that allow the display of tooltips
     */
    @FXML
    private void initListAndTable() {
        //ObservableList<String> fileNames = FXCollections.observableArrayList("No root selected");

        // this is the callback that is used to create single cells, including a tooltip that shows the complete cell value
        Callback<TableColumn<TableEntry, String>, TableCell<TableEntry, String>> cell = new Callback<>() {
            @Override
            public TableCell<TableEntry, String> call(TableColumn<TableEntry, String> tableEntryStringTableColumn) {
                return new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item);
                        if (item == null || item.equals("")) item = "none";
                        Tooltip tip = new Tooltip(item);
                        tip.setShowDelay(new Duration(300));
                        setTooltip(tip);
                    }
                };
            }
        };

        TableColumn<TableEntry, String> keyColumn = new TableColumn<>("Key");
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("keyword"));
        keyColumn.setCellFactory(cell);

        TableColumn<TableEntry, String> authorColumn = new TableColumn<>("Author/s");
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        authorColumn.setCellFactory(cell);

        TableColumn<TableEntry, String> titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleColumn.setCellFactory(cell);

        TableColumn<TableEntry, String> yearColumn = new TableColumn<>("Year");
        yearColumn.setCellValueFactory(new PropertyValueFactory<>("year"));
        yearColumn.setCellFactory(cell);

        bibTable.getColumns().add(keyColumn);
        bibTable.getColumns().add(authorColumn);
        bibTable.getColumns().add(titleColumn);
        bibTable.getColumns().add(yearColumn);
        bibTable.setPlaceholder(new Label("No Data to display yet!"));

        bibTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bibTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

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
     * Creates a listener that detects keyboard inputs, with the help of jnativehook
     * Detects key input of F1 to auto-press ctrl/cmd + a +c to copy a bib entry
     */
    private void initShortCutListener() {
        // Get the logger for "org.jnativehook" and disable warnings
        LogManager.getLogManager().reset();
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);

        // Don't forget to disable the parent handlers.
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            System.err.println("Problem registering native hook / Key hook");
        }
        GlobalScreen.addNativeKeyListener(new ShortCutListener());
    }

    /**
     * This function will add Listeners to the height and width property of the window,
     * in order to scale elements like the buttons to smaller window sizes
     *
     * @param stage stage from creation of the App window
     */
    public void setStageAndListeners(Stage stage) {
        this.stage = stage;

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            // System.out.println("New height: " + newVal);
            this.height = newVal.doubleValue();
            updateSize();
        });

        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            //System.out.println("New width: " + newVal);
            this.width = newVal.doubleValue();
            updateSize();
        });
    }

    /**
     * Updates the size of the button elements in the window, depending on
     * the width AND height, if either is too small, buttons will be shrunk
     */
    private void updateSize() {
        final String smallStyle = "-fx-padding: 0;\n" +
                "    -fx-min-height: 32px;\n" + // change to 40 if another button is added
                "    -fx-pref-width: 32px;\n" +
                "    -fx-min-width: 32px;\n" +
                "    -fx-background-size: 22px;\n" +
                "    -fx-background-repeat: no-repeat;\n" +
                "    -fx-background-position: center;";

        final String largeStyle = "-fx-padding: 0;\n" +
                "    -fx-min-height: 45px;\n" +
                "    -fx-min-width: 45px;\n" +
                "    -fx-pref-width: 45px;\n" +
                "    -fx-background-size: 35px;\n" +
                "    -fx-background-repeat: no-repeat;\n" +
                "    -fx-background-position: center;";

        final String smallStyleEnableButton = "   -fx-min-height: 150;\n" +
                "    -fx-min-width: 14;\n" +
                "    -fx-pref-width: 14;\n" +
                "-fx-font-size: 12";

        final String largeStyleEnableButton = "   -fx-min-height: 230;\n" +
                "    -fx-min-width: 17;\n" +
                "    -fx-pref-width: 17;\n" +
                "-fx-font-size: 16";

        if (!this.isBelowSize && ((isFileListActive ? this.width <= 785 : this.width <= 600) || this.height <= 650)) {
            this.isBelowSize = true;
            for (Node node : this.buttons
            ) {
                if (node.getClass() == Button.class) {
                    node.setStyle(smallStyle);
                }
            }
            buttonBox3.setSpacing(3);
            buttonBox2.setSpacing(3);
            enableFileListButton.setStyle(smallStyleEnableButton);

        } else if (this.isBelowSize && ((isFileListActive ? this.width > 785 : this.width > 600) && this.height > 650)) {
            this.isBelowSize = false;
            for (Node node : this.buttons
            ) {
                if (node.getClass() == Button.class) {
                    node.setStyle(largeStyle);
                }
            }
            buttonBox3.setSpacing(8);
            buttonBox2.setSpacing(8);
            enableFileListButton.setStyle(largeStyleEnableButton);
        }
    }

    /**
     * Adds all buttons in the app to a single list for easier changing
     */
    private void getAllButtons() {
        //this.buttons.addAll(buttonBox1.getChildren());
        for (Node n : buttonBox2.getChildren()) {
            if (n.getClass() == Button.class) {
                buttons.add(n);
            }
        }
        this.buttons.addAll(buttonBox3.getChildren());
        this.buttons.add(insertButton);
        this.buttons.add(selectFile);
    }

    /**
     * Currently unused, might be needed later for a second scene
     *
     * @throws IOException .
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
        String selectedFileName = fileManager.getSelectedFileName();
        fileManager.selectSingleFile(actionEvent);
        if (!selectedFileName.equals(fileManager.getSelectedFileName())) {
            this.secondList.setText("Entries inside: " + fileManager.getSelectedFileName());
            fileManager.readFileIntoTable(bibTable);
        }
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
            this.secondList.setText("Entries inside: " + fileManager.getSelectedFileName());
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
        this.secondList.setText("Entries inside: " + fileManager.getSelectedFileName());
        fileManager.readFileIntoTable(bibTable);
    }


    /**
     * Will call the fileManger to insert all entries inside the textArea
     * This runs on an extra thread to prevent UI from blocking on large inserts
     * Throw an alert if there is no file selected
     *
     * @param actionEvent click button
     */
    public void insertIntoFile(ActionEvent actionEvent) {
        if (!fileManager.isFileSelected()) {
            throwAlert("Entry/ies not inserted!", "Select a file first!");
        } else {
            fileManager.insertIntoFile(inputArea.getText(), bibTable.getItems(), actionEvent);
        }
    }

    /**
     * Displays an alert box that can be dismissed with an OK-button
     * Styles are defined in corresponding dark/light css
     *
     * @param msg msg to display inside the alert box
     */
    public static void throwAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.OK);
        alert.setTitle(title);
        alert.getDialogPane().getScene().getStylesheets()
                .add(App.class.getResource(isDarkMode ? "darkStyles.css" : "lightStyles.css").toExternalForm());
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(App.class.getResource("icon/icon.png").toExternalForm()));
        alert.show();
    }

    /**
     * Will put the selected item(s) into the TextArea for editing
     *
     * @param actionEvent click button
     */
    public void selectEntry(ActionEvent actionEvent) {
        if (fileManager.isFileSelected()) {
            ObservableList<TableEntry> items = bibTable.getSelectionModel().getSelectedItems();
            if (items.size() == 0) {
                throwAlert("No entry selected!", "Select an entry from the table first!");
                return;
            }
            String oldText = this.inputArea.getText();
            StringBuilder builder = new StringBuilder(oldText.length() < 30 ? "" : oldText + "\n\n");

            for (TableEntry tableEntry : items
            ) {
                String entry = fileManager.getBibEntry(tableEntry.getKeyword());
                if (!oldText.contains(entry.replaceAll("\r", "").trim())) {
                    builder.append(FormatChecker.replaceValueClosures(entry, toCurlyMode)).append("\n\n");
                }
            }
            builder.setLength(builder.length() - 2);
            this.inputArea.setText(builder.toString());

        } else {
            throwAlert("No file selected!", "Select a file first!");
        }
    }

    /**
     * Will delete all entries, that are selected in the table, from the file
     * User will be asked if he is sure to delete x amount of entries in alert dialog
     *
     * @param actionEvent click button
     */
    public void deleteEntry(ActionEvent actionEvent) {
        if (!fileManager.isFileSelected()) {
            throwAlert("File/s not deleted!", "Select a file first!");
            return;
        }

        ObservableList<TableEntry> selectedItems = bibTable.getSelectionModel().getSelectedItems();
        if (selectedItems.size() < 1) {
            throwAlert("File/s not deleted!", "Nothing selected!");
            return;
        }

        String msg = selectedItems.size() == 1 ? "Delete 1 entry from the list?" :
                "Delete " + selectedItems.size() + " entries from the list?";

        Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.YES, ButtonType.CANCEL);
        alert.setTitle("Delete confirmation");
        alert.getDialogPane().getScene().getStylesheets()
                .add(App.class.getResource(isDarkMode ? "darkStyles.css" : "lightStyles.css").toExternalForm());
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(App.class.getResource("icon/icon.png").toExternalForm()));
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                ObservableList<TableEntry> items = bibTable.getSelectionModel().getSelectedItems();
                if (items != null && items.size() > 0) {
                    ArrayList<String> keywords = new ArrayList<>();
                    for (TableEntry e : items) {
                        keywords.add(e.getKeyword());
                    }
                    bibTable.getItems().removeAll(items);
                    fileManager.deleteEntriesFromFile(keywords);
                } else {
                    throwAlert("File/s not deleted!", "Nothing selected!");
                }
            }
        });
    }

    /**
     * Will open the selected Entries in the browser based on their URL, if
     * no URL is found based on their DOI nad if that is also not found based on
     * a search of title + author.
     * A search engine needs to be specified when there is no clear URL.
     * If none is found throw an alert
     *
     * @param actionEvent button press
     */
    public void openInBrowser(ActionEvent actionEvent) {
        if (!fileManager.isFileSelected()) {
            throwAlert("Cant open in browser", "Select a file first!");
            return;
        }

        ObservableList<TableEntry> selectedItems = bibTable.getSelectionModel().getSelectedItems();
        if (selectedItems.size() < 1) {
            throwAlert("Cant open in browser", "Select an entry first!");
            return;
        }

        HostServices service = App.getInstance().getHostServices();

        for (TableEntry entry : selectedItems
        ) {
            if (!entry.getUrl().equals("none") && entry.getUrl().length() > 2) {
                service.showDocument(entry.getUrl());

            } else if (!entry.getDoi().equals("none") && entry.getDoi().length() > 2) {
                service.showDocument("https://doi.org/" + entry.getDoi());

            } else if (entry.getKeyword().startsWith("10.") && entry.getKeyword().charAt(7) == '/') {
                service.showDocument("https://doi.org/" + entry.getKeyword());

            } else if (!entry.getTitle().equals("none") && !entry.getAuthor().equals("none")) { //todo add config for search engine!
                String engine = "https://duckduckgo.com/?q="; // or GOOGLE: https://www.google.at/search?q=
                String query = entry.getTitle().trim() + "+" + entry.getAuthor().trim();
                query = query.replaceAll(" ", "+").replaceAll("[\\[\\]{}|\\\\”%~#<>$–_.!*‘()]", "");

                service.showDocument(engine + query);
            } else {
                throwAlert("Cant open in browser", "Not enough information to search!");
            }
        }
    }

    public void replaceQuotationMarks(ActionEvent actionEvent) {
        replaceValueClosures(true, actionEvent);
    }

    public void replaceCurlyBraces(ActionEvent actionEvent) {
        replaceValueClosures(false, actionEvent);
    }

    /**
     * Will call the function in FileManager to replace quotation marks or curly braces
     * Throws alert when no file is selected and will prompt the user to confirm the replacement
     */
    private void replaceValueClosures(boolean toCurlyBraces, ActionEvent actionEvent) {
        if (!fileManager.isFileSelected()) {
            throwAlert("Cant replace anything", "Select a file first!");
            return;
        }
        String msg = toCurlyBraces ? "Do you really want to replace all outer \n\" \" in this file with { }?" :
                "Do you really want to replace all outer \n{ } in this file with \" \"?";
        Alert alert = new Alert(Alert.AlertType.NONE, msg, ButtonType.YES, ButtonType.CANCEL);
        alert.setTitle("Replacement confirmation");
        alert.getDialogPane().getScene().getStylesheets()
                .add(App.class.getResource(isDarkMode ? "darkStyles.css" : "lightStyles.css").toExternalForm());
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.getIcons().add(new Image(App.class.getResource("icon/icon.png").toExternalForm()));
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                fileManager.replaceValueClosures(toCurlyBraces, actionEvent);
            }
        });
    }

    /**
     * Switches the current color theme between dark and light
     * The default theme is dark
     *
     * @param actionEvent button click
     */
    public void changeColorMode(ActionEvent actionEvent) {
        ObservableList<String> styleSheets = this.stage.getScene().getStylesheets();
        styleSheets.clear();
        styleSheets.add(getClass().getResource("styles.css").toExternalForm());
        styleSheets.add(getClass().getResource(isDarkMode ? "lightStyles.css" : "darkStyles.css").toExternalForm());
        isDarkMode = !isDarkMode;
    }

    /**
     * Disables or enables the background clipboard service
     * This is helpful when you want to copy around bibEntries without
     * adding them to the text-field
     *
     * @param actionEvent button click
     */
    public void toggleAutoInsert(ActionEvent actionEvent) {
        if (this.clipboardService.isRunning()) {
            this.clipboardService.cancel();
            this.toggleAutoInsert.setId("toggleAutoInsert2");
        } else {
            this.clipboardService.restart();
            this.toggleAutoInsert.setId("toggleAutoInsert");
        }
    }

    /**
     * Removes all text from the TextArea
     *
     * @param actionEvent button click
     */
    public void clearTextArea(ActionEvent actionEvent) {
        this.inputArea.setText("Content cleared.");
    }

    /**
     * Selects all items in the bibTable
     *
     * @param actionEvent button click
     */
    public void selectAllEntries(ActionEvent actionEvent) {
        if (!fileManager.isFileSelected()) {
            throwAlert("Entry/ies not inserted!", "Select a file first!");
        } else {
            if (bibTable.getSelectionModel().getSelectedItems().size() == bibTable.getItems().size()) {
                bibTable.getSelectionModel().clearSelection();
            } else {
                bibTable.getSelectionModel().selectAll();
            }
        }
    }

    /**
     * Switch between the formatting type for entries to either use
     * "" OR {} for values.
     *
     * @param actionEvent button click
     */
    public void changeClosureMode(ActionEvent actionEvent) {
        toCurlyMode = !toCurlyMode;
        if (toCurlyMode) this.changeClosureMode.setId("changeClosureMode");
        else this.changeClosureMode.setId("changeClosureMode2");
    }

    public static boolean isToCurlyMode() {
        return toCurlyMode;
    }

    /**
     * Will disable the ListView to the left of the App, to make more space for the
     * Table, also enables a hidden button to enable the ListView in again
     *
     * @param actionEvent button click
     */
    public void disableFileList(ActionEvent actionEvent) {
        isFileListActive = false;

        colWithListView.setVisible(false);
        colWithListView.setManaged(false);
        selectFile.setVisible(false);
        selectFile.setManaged(false);
        secondColumnRegion.setVisible(false);
        secondColumnRegion.setManaged(false);

        listViewColumnConstraints.setMinWidth(0);
        secondColumnConstraints.setMinWidth(50);
        secondColumnConstraints.setPrefWidth(50);
        secondColumnConstraints.setPercentWidth(2);

        enableFileListButton.setVisible(true);
        enableFileListButton.setManaged(true);
        enableFileListButton.setTranslateX(-10);

        this.secondColumnBox.setAlignment(Pos.CENTER_LEFT);
    }

    /**
     * Inverts the changes of the above function
     *
     * @param actionEvent button click
     */
    public void enableFileList(ActionEvent actionEvent) {
        isFileListActive = true;

        colWithListView.setVisible(true);
        colWithListView.setManaged(true);
        selectFile.setVisible(true);
        selectFile.setManaged(true);
        secondColumnRegion.setVisible(true);
        secondColumnRegion.setManaged(true);

        listViewColumnConstraints.setMinWidth(220);
        secondColumnConstraints.setPrefWidth(50);
        secondColumnConstraints.setPercentWidth(7);

        enableFileListButton.setVisible(false);
        enableFileListButton.setManaged(false);

        secondColumnBox.setAlignment(Pos.CENTER);
    }

    /**
     * Will undo the last move (only file operations), with the class the UndoRedoManager
     * After the undo a alert with the appropriate undone action is shown
     *
     * @param actionEvent button click
     */
    public void undoLastMove(ActionEvent actionEvent) {
        if (undoRedo.isUndoPossible()) {
            UndoRedoManager.Action action = undoRedo.undoLastFileOperation();

            fileManager.readFileIntoTable(this.bibTable);
            this.secondList.setText("Entries inside: " + fileManager.getSelectedFileName());

            switch (action) {
                case INIT:
                    throwAlert("Undo success!", "Last available file operation (insert) was undone!");
                    break;
                case WRITE:
                    throwAlert("Undo success!", "The last file operation (insert) was undone!");
                    break;
                case DELETE:
                    throwAlert("Undo success!", "The last file operation (delete) was undone!");
                    break;
                case REFORMAT:
                    throwAlert("Undo success!", "The last file operation (reformat) was undone");
                    break;
                case NONE_LEFT:
                    throwAlert("Undo error!", "Cant undo any more operations!");
                    break;
                case ERROR:
                    throwAlert("Undo error!", "Error occurred during undo, nothing happened!");
            }

            undoMoveButton.setDisable(!undoRedo.isUndoPossible());
            redoMoveButton.setDisable(!undoRedo.isRedoPossible());
        } else {
            throwAlert("Undo error!", "Cant undo any more operations!");
        }
    }

    /**
     * Redoes the last undone move, with the class UndoRedoManager
     * As with the function above an alert wil be shown
     *
     * @param actionEvent button click
     */
    public void redoLastMove(ActionEvent actionEvent) {
        if (undoRedo.isRedoPossible()) {
            UndoRedoManager.Action action = undoRedo.redoLastFileOperation();

            fileManager.readFileIntoTable(this.bibTable);
            this.secondList.setText("Entries inside: " + fileManager.getSelectedFileName());

            switch (action) {
                case WRITE:
                    throwAlert("Redo success!", "Previous file operation (insert) was redone!");
                    break;
                case DELETE:
                    throwAlert("Redo success!", "Previous file operation (delete) was redone!");
                    break;
                case REFORMAT:
                    throwAlert("Redo success!", "Previous file operation (reformat) was redone");
                    break;
                case NONE_LEFT:
                    throwAlert("Redo error!", "Cant redo any more operations!");
                    break;
                case ERROR:
                    throwAlert("Redo error!", "Error occurred during redo, nothing happened!");
            }
            undoMoveButton.setDisable(!undoRedo.isUndoPossible());
            redoMoveButton.setDisable(!undoRedo.isRedoPossible());

        } else {
            throwAlert("Redo error!", "Cant redo any more operations!");
        }
    }
}
