package org.wst.helper;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.wst.App;
import org.wst.PrimaryController;
import org.wst.model.TableEntry;

import java.io.*;
import java.util.*;

/**
 * Singleton Class!
 */
public class FileManager {
    private FileManager() {
    }

    private final static FileManager fileManager = new FileManager();

    public static FileManager getInstance() {
        return fileManager;
    }

    private File rootDirectory;
    private Task<List<File>> directorySearchTask;
    private List<File> filesInsideRoot;
    private File selectedFile;
    private Map<String, String> bibMap;
    private String fileAsString;
    private UndoRedoManager undoRedo;
    private Button undoButton, redoButton;
    private final Object lock = new Object();

    /**
     * Needed by UndoRedoManager, in order to make fileWrites on the same lock
     *
     * @return lock
     */
    public Object getLock() {
        return lock;
    }

    public void setUndoRedoButtons(Button undo, Button redo) {
        this.undoButton = undo;
        this.redoButton = redo;
        this.undoRedo = UndoRedoManager.getInstance();
    }

    /**
     * TODO: improve the way duplicate filenames are handled!
     * Button is disabled during processing
     * Will prompt the user to choose a directory, with possible .bib files
     * If a directory has already been chosen, the same directory will be opened
     * in the dialog. The directory and every subdirectory will be searched for
     * .bib files, that will be added to the ListView.
     * <p>
     * The search task will happen on a separate Thread to the UI Thread!
     *
     * @param actionEvent click button event
     * @param view        the list to update
     */
    public void selectDirectory(ActionEvent actionEvent, ListView<String> view) {
        Button b = (Button) actionEvent.getSource();
        String id = b.getId();
        if (id.equals("searchButton")) {

            Alert alert = new Alert(Alert.AlertType.NONE,
                    "The search for .bib entries will be canceled!",
                    ButtonType.OK, ButtonType.CANCEL);
            alert.setTitle("Do you want to cancel?");
            alert.getDialogPane().getScene().getStylesheets()
                    .add(App.class.getResource(PrimaryController.isDarkMode ? "darkStyles.css" : "lightStyles.css").toExternalForm());
            Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
            stage.getIcons().add(new Image(App.class.getResource("icon/icon.png").toExternalForm()));
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    b.setId("selectRootButton");
                    directorySearchTask.cancel();
                }
            });
            return;
        }
        //b.setDisable(true);
        b.setId("searchButton");

        DirectoryChooser chooser = new DirectoryChooser();

        // open chooser at last selected folder..
        if (rootDirectory != null) chooser.setInitialDirectory(rootDirectory);
        chooser.setTitle("Select a root directory");
        rootDirectory = chooser.showDialog(b.getScene().getWindow());

        if (rootDirectory != null) {
            directorySearchTask = getFileSearchTask(rootDirectory);
            directorySearchTask.setOnSucceeded(list -> {
                List<String> fileNames = new ArrayList<>();

                filesInsideRoot = directorySearchTask.getValue();
                for (File f : filesInsideRoot
                ) {
                    fileNames.add(f.getParentFile().getName() + File.separator + f.getName());
                }

                Collections.sort(fileNames);
                System.out.println(fileNames);
                if (fileNames.size() == 0) fileNames.add("No .bib files found!");
                view.setItems(FXCollections.observableArrayList(fileNames));

                b.setDisable(false);
                b.setId(id);

            });
            Thread th = new Thread(directorySearchTask);
            th.start();
        } else {
            //b.setDisable(false);
            b.setId(id);
        }
    }

    /**
     * Task for the search thread
     *
     * @param dir root directory
     * @return List of all .bib files inside root
     */
    private Task<List<File>> getFileSearchTask(File dir) {
        return new Task<>() {
            @Override
            protected List<File> call() {
                return getAllSubFiles(dir, new ArrayList<>());
            }
        };
    }

    /**
     * Recursive function that will call itself until every .bib file is found inside the current directory
     *
     * @param dir  current parent directory
     * @param list current list of .bib files
     * @return subList of .bib Files
     */
    private List<File> getAllSubFiles(File dir, List<File> list) {
        if (directorySearchTask.isCancelled()) return list;
        File[] sub = dir.listFiles();
        if (sub != null) {
            for (File f : sub
            ) {
                if (f.isFile() && f.getName().endsWith(".bib") && !list.contains(f)) {
                    list.add(f);
                } else if (f.isDirectory()) {
                    getAllSubFiles(f, list);
                }
            }
        }
        return list;
    }

    /**
     * @return the full path of the current root directory or else "No root selected"
     */
    public String getRootDirectory() {
        return rootDirectory != null ? rootDirectory.getAbsolutePath() : "No root selected";
    }

    /**
     * Will prompt the user to select a single .bib file from the file system,
     * will open same directory if called multiple times
     * Button is disabled during selection
     *
     * @param actionEvent click button
     */
    public void selectSingleFile(ActionEvent actionEvent) {
        Button b = (Button) actionEvent.getSource();
        String id = b.getId();
        b.setDisable(true);
        b.setId("searchButton");

        FileChooser fileChooser = new FileChooser();

        if (selectedFile != null) {
            fileChooser.setInitialDirectory(selectedFile.getParentFile());
        }
        fileChooser.setTitle("Select a single file");
        File tmp = fileChooser.showOpenDialog(b.getScene().getWindow());
        if (tmp != null) selectedFile = tmp;
        b.setDisable(false);
        b.setId(id);
    }

    /**
     * Will be called when user chooses a file from the current list of .bib files
     *
     * @param filename name of the file to select
     */
    public void selectFileFromList(String filename) {
        if (filesInsideRoot == null) return;
        for (File f : filesInsideRoot
        ) {
            if ((f.getParentFile().getName() + File.separator + f.getName()).equals(filename)) {
                selectedFile = f;
            }
        }
    }

    /**
     * @return the name of the currently selected file or "No file" if none has been selected yet
     */
    public String getSelectedFileName() {
        return selectedFile != null ? selectedFile.getName() : "No file";
    }

    public boolean isFileSelected() {
        return selectedFile != null;
    }

    public void setSelectedFile(File selectedFile) {
        this.selectedFile = selectedFile;
    }

    /**
     * Will write the given bib-entry into the selected file, validity should be checked before calling this function
     * If an entry with a given keyword is already in the file it will replace the one in the file and the map
     * Synchronized on lock object, only one thread may write to a file change the file-String at a time
     * -> prevent any possible exceptions
     * Will remove any empty lines at the start and end of the file
     *
     * @param str bib entry to write
     */
    public void writeSingleEntryToFile(String str) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                synchronized (lock) {
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), true));

                        String keyword = FormatChecker.getBibEntryKeyword(str);
                        if (keyword == null) {
                            System.err.println("Cant insert invalid bib entry!");
                        } else if (bibMap.containsKey(keyword)) { // entry already in file, overwrite whole file, including new entry
                            String toReplace = bibMap.get(keyword);
                            fileAsString = fileAsString.replace(toReplace, str).trim() + "\r\n";
                            bibMap.put(keyword, str);
                            writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), false));
                            writer.write(fileAsString);

                        } else { // entry not in file append it
                            bibMap.put(keyword, str);
                            fileAsString += "\r\n" + str;
                            writer.write("\r\n");
                            writer.write(str);
                        }
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                        System.err.println("Error writing to file");
                        e.printStackTrace();
                    }
                    return null;
                }
            }
        };
        Thread th = new Thread(task);
        th.start();
    }

    /**
     * This will rewrite the file without the selected Entries, deleting them this way
     * If an entry is not part of the bibMap nothing will happen with that entry
     * Will remove any empty lines at the start and end (except 1) of the file
     * Will remove any double empty lines from the file
     * Synchronized on lock object, only one thread may write to a file change the file-String at a time
     *
     * @param keywords entry keyword to delete
     */
    public void deleteEntriesFromFile(List<String> keywords) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                synchronized (lock) {
                    try {
                        for (String key : keywords
                        ) {
                            if (bibMap.containsKey(key)) {
                                String toReplace = bibMap.get(key);
                                if (fileAsString.contains("\r\n" + toReplace)) {
                                    fileAsString = fileAsString.replace("\r\n" + toReplace, "");
                                } else {
                                    fileAsString = fileAsString.replace(toReplace, "");
                                }
                                bibMap.remove(key);
                            } else {
                                System.err.println("Cant delete entry: '" + key + "' its not in the file");
                            }
                        }
                        BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), false));
                        fileAsString = fileAsString.trim() + "\r\n";
                        writer.write(fileAsString);
                        writer.flush();
                        writer.close();
                        undoRedo.saveOperation(fileAsString, selectedFile, UndoRedoManager.Action.DELETE);
                    } catch (IOException e) {
                        System.err.println("Error writing to file");
                        e.printStackTrace();
                    }
                    undoButton.setDisable(!undoRedo.isUndoPossible());
                    redoButton.setDisable(!undoRedo.isRedoPossible());
                    return null;
                }
            }
        };
        Thread th = new Thread(task);
        th.start();
    }

    /**
     * Will check if the current entries inside the TextArea are valid and insert them all
     * into the file, if all entries are new they will be appended to the file, else the file is rewritten
     * The map of bib entries will also be updated accordingly
     * Will also update the TableView with new/updated entries
     * Any change of fileAsString and write operation is synchronized on variable "lock"
     * Throw an alert if there are no entries to insert, and one telling how many entries where inserted
     *
     * @param text    text block from textArea
     * @param entries entries inside the table for updates
     * @param event   button press event
     */
    public void insertIntoFile(String text, ObservableList<TableEntry> entries, ActionEvent event) {
        Button b = (Button) event.getSource();
        b.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {

                ArrayList<String> entryArray = FormatChecker.getBibEntries(text);
                if (entryArray.isEmpty()) {
                    Platform.runLater(() -> PrimaryController.throwAlert("Entry/ies not inserted!", "No valid entry found!"));
                } else {
                    boolean needsRewrite = false;
                    String fileAsStringTmp = fileAsString;
                    StringBuilder rewriteBuilder = new StringBuilder(fileAsStringTmp);
                    StringBuilder appendBuilder = new StringBuilder();

                    for (String entry : entryArray) {
                        String keyword = FormatChecker.getBibEntryKeyword(entry);
                        if (keyword == null) {
                            System.err.println("Insert into file, invalid entry");
                        } else {
                            if (bibMap.containsKey(keyword)) { // entry already in file, replace old one
                                String toReplace = bibMap.get(keyword);
                                fileAsStringTmp = rewriteBuilder.toString();
                                fileAsStringTmp = fileAsStringTmp.replace(toReplace, entry).trim() + "\r\n";
                                rewriteBuilder = new StringBuilder(fileAsStringTmp);
                                bibMap.put(keyword, entry);
                                needsRewrite = true;

                            } else { // entry not in file yet append it
                                bibMap.put(keyword, entry);
                                rewriteBuilder.append("\r\n").append(entry);
                                appendBuilder.append("\r\n").append(entry);
                            }

                            boolean isAlreadyInFile = false; // update Table entries with this entry
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
                    }

                    synchronized (lock) {
                        try {
                            BufferedWriter writer;
                            if (needsRewrite) {
                                writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), false));
                                writer.write(rewriteBuilder.toString());
                            } else {
                                writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), true));
                                writer.write(appendBuilder.toString());
                            }
                            writer.flush();
                            writer.close();
                            fileAsString = rewriteBuilder.toString();
                        } catch (IOException e) {
                            System.err.println("Error during insert file write");
                            e.printStackTrace();
                        }
                    }

                    undoRedo.saveOperation(fileAsString, selectedFile, UndoRedoManager.Action.WRITE);
                    //inputArea.setText("Bib entry successfully inserted into " + fileManager.getSelectedFileName());
                    boolean isSingle = entryArray.size() == 1;
                    Platform.runLater(() -> {
                        PrimaryController.throwAlert(isSingle ? "Bib-Entry inserted!" : "Entries inserted!",
                                isSingle ? "The single Bib-Entry was inserted" : entryArray.size() + " Bib-Entries successfully inserted");
                        undoButton.setDisable(!undoRedo.isUndoPossible());
                        redoButton.setDisable(!undoRedo.isRedoPossible());
                    });

                }
                b.setDisable(false);
                return null;
            }
        };
        Thread th = new Thread(task);
        th.start();

    }

    // todo add config to disable overwriting files?

    /**
     * Will create a new .bib file by prompting the user to select a location
     * An empty string will be written to the file to make sure it is created
     * <p>
     * If the file already exists it will be overwritten, the OS should ask the user
     * if he wants to overwrite the file...
     *
     * @param actionEvent click button event
     * @return true if file was created, else false
     */
    public boolean createFile(ActionEvent actionEvent) {
        Button b = (Button) actionEvent.getSource();
        String id = b.getId();
        b.setDisable(true);
        b.setId("searchButton");

        FileChooser fileChooser = new FileChooser();
        if (selectedFile != null) {
            fileChooser.setInitialDirectory(selectedFile.getParentFile());
        }
        fileChooser.setTitle("Create a new file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BibTeX Files", "*.bib"));
        fileChooser.setInitialFileName("bibliography.bib");

        File tmp = fileChooser.showSaveDialog(b.getScene().getWindow());
        b.setDisable(false);
        b.setId(id);
        if (tmp != null) {
            selectedFile = tmp;

            try {
                if (!selectedFile.createNewFile()) {
                    FileWriter fw = new FileWriter(selectedFile, false);
                    fw.write("");
                    fw.flush();
                    fw.close();
                }
            } catch (IOException e) {
                System.err.println("Error writing file, during creation");
                e.printStackTrace();
            }
            return true;
        } else return false;
    }

    /**
     * Will read the selected file and check block wise if there is a bib entry. If there
     * is an entry it will be added to the bibMap for later use.
     * The complete File will be saved as String, in order to rewrite it later.
     * This Method will fill the table with the data from the file
     * <p>
     * Duplicates (same entry keyword) will only occur a single time in the map!
     * Synchronized to prevent bugs from quickly loading various large files
     */
    public synchronized void readFileIntoTable(TableView<TableEntry> view) {
        Task<ObservableList<TableEntry>> task = new Task<>() {
            @Override
            protected ObservableList<TableEntry> call() throws Exception {
                ObservableList<TableEntry> entries = FXCollections.observableArrayList();
                bibMap = new TreeMap<>();
                if (selectedFile == null) {
                    entries.add(new TableEntry(TableEntry.Error.FILE_NOT_FOUND));
                    return entries;
                }
                try {
                    FileReader fr = new FileReader(selectedFile);
                    BufferedReader reader = new BufferedReader(fr);
                    String line, entry;
                    StringBuilder builder = new StringBuilder();
                    StringBuilder bibBuilder = new StringBuilder();

                    String line2;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\r\n");
                        bibBuilder.append(line).append("\r\n");
                        // ignore comment lines starting with '%'
                        if ((!line.startsWith("%")) && bibBuilder.toString().contains("@")) {
                            while ((line2 = reader.readLine()) != null) {
                                builder.append(line2).append("\r\n");
                                bibBuilder.append(line2).append("\r\n");
                                if (line2.contains("@")) {
                                    line = line2;
                                    break;
                                }
                            }
                            // if entry is valid and not in map add it
                            if (!(entry = FormatChecker.basicBibTeXCheck(bibBuilder.toString())).isEmpty()) {

                                TableEntry tableEntry = FormatChecker.getBibTableEntry(entry);
                                if (tableEntry != null) {
                                    bibMap.put(tableEntry.getKeyword(), entry);
                                    entries.add(tableEntry);
                                    bibBuilder.setLength(0);
                                    bibBuilder.append(line).append("\r\n");
                                }
                            }
                        }
                    }
                    reader.close();
                    fileAsString = builder.toString();
                    if (undoRedo.isInit()) {
                        undoRedo.saveOperation(fileAsString, selectedFile, UndoRedoManager.Action.INIT);
                    }

                } catch (IOException e) {
                    System.err.println("Error reading from file!");
                    e.printStackTrace();
                    entries.add(new TableEntry(TableEntry.Error.FILE_READ_ERROR));
                }

                if (entries.size() == 0) {
                    entries.add(new TableEntry(TableEntry.Error.NO_ENTRIES_FOUND));
                }
                return entries;
            }
        };
        task.setOnSucceeded(list -> {
            ObservableList<TableEntry> result = task.getValue();
            switch (result.get(0).getError()) {
                case NO_ENTRIES_FOUND:
                    view.getItems().clear();
                    view.setPlaceholder(new Label("No entries inside selected file!"));
                    break;
                case FILE_NOT_FOUND:
                    view.getItems().clear();
                    view.setPlaceholder(new Label("File could not be found!"));
                    break;
                case FILE_READ_ERROR:
                    view.getItems().clear();
                    view.setPlaceholder(new Label("Error while reading the file!"));
                    break;
                case NONE:
                    view.setItems(result);
            }
        });

        Thread th = new Thread(task);
        th.start();

    }

    /**
     * Will search the bibMap for the selected Item and then return
     * the corresponding Bib-Entry
     *
     * @param keyword item selected from bibList
     * @return selected Bib-Entry
     */
    public String getBibEntry(String keyword) {
        if (bibMap == null || bibMap.isEmpty()) {
            return "";
        } else {
            String entry = bibMap.get(keyword);
            if (entry == null) {
                return "";
            }
            return FormatChecker.basicBibTeXCheck(entry);
        }
    }

    /**
     * This function will replace the quotation marks of every tag and value pair with curly brackets
     * OR THE OTHER WAY AROUND!
     * This will happen in the whole file that is currently selected
     * e.g. [tag = "x"] will be replaced with [tag = {x}] OR other way around
     * <p>
     * Synchronized on lock object, only one thread may write to a file change the file-String at a time
     */
    public void replaceValueClosures(boolean toCurlyBraces, ActionEvent actionEvent) {
        Button b = (Button) actionEvent.getSource();
        b.setDisable(true);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                synchronized (lock) {
                    try {
                        BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), false));

                        for (Map.Entry<String, String> entry : bibMap.entrySet()
                        ) {
                            String oldEntry = entry.getValue();
                            String newValue = FormatChecker.replaceValueClosures(oldEntry, toCurlyBraces) + "\r\n";
                            fileAsString = fileAsString.replace(oldEntry.trim(), newValue.trim());
                            bibMap.put(entry.getKey(), newValue);
                        }
                        writer.write(fileAsString.trim() + "\r\n");
                        writer.flush();
                        writer.close();
                        undoRedo.saveOperation(fileAsString, selectedFile, UndoRedoManager.Action.REFORMAT);
                        undoButton.setDisable(!undoRedo.isUndoPossible());
                        redoButton.setDisable(!undoRedo.isRedoPossible());

                    } catch (IOException e) {
                        System.err.println("Error writing to file");
                        e.printStackTrace();
                    }
                    b.setDisable(false);
                    return null;
                }
            }
        };
        Thread th = new Thread(task);
        th.start();
    }

}