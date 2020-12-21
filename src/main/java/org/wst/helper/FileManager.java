package org.wst.helper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.wst.model.TableEntry;

import java.io.*;
import java.util.*;

/**
 * Singleton Class!
 */
public class FileManager {
    private FileManager() {
    }

    private static FileManager fileManager = null;

    public static FileManager getInstance() {
        if (fileManager == null) {
            fileManager = new FileManager();
        }
        return fileManager;
    }

    private File rootDirectory;
    private List<File> filesInsideRoot;
    private File selectedFile;
    private Map<String, String> bibMap;
    private String fileAsString;

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
        b.setDisable(true);
        b.setId("searchButton");

        DirectoryChooser chooser = new DirectoryChooser();

        // open chooser at last selected folder..
        if (rootDirectory != null) chooser.setInitialDirectory(rootDirectory);
        chooser.setTitle("Select a root directory");
        rootDirectory = chooser.showDialog(b.getScene().getWindow());

        if (rootDirectory != null) {
            Task<List<File>> t = getFileSearchTask(rootDirectory);
            t.setOnSucceeded(list -> {
                List<String> fileNames = new ArrayList<>();

                filesInsideRoot = t.getValue();
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
            Thread th = new Thread(t);
            th.start();
        } else {
            b.setDisable(false);
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
        selectedFile = fileChooser.showOpenDialog(b.getScene().getWindow());
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

    /**
     * Will write the given bib-entry into the selected file, validity should be checked before calling this function
     * If an entry with a given keyword is already in the file it will replace the on in the file and the map
     * Synchronized, only one thread my write to a file at a time/ prevent any possible exceptions
     *
     * @param str bib entry to write
     */
    public synchronized void writeToFile(String str) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), true));

                    String keyword = FormatChecker.getBibEntryKeyword(str);
                    if (keyword == null) {
                        System.err.println("Cant insert invalid bib entry!");
                    } else if (bibMap.containsKey(keyword)) { // entry already in file, overwrite whole file, including new entry
                        String toReplace = bibMap.get(keyword);
                        fileAsString = fileAsString.replace(toReplace, str);
                        bibMap.put(keyword, str);
                        writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), false));
                        writer.write(fileAsString);

                    } else { // entry not in file append it
                        bibMap.put(keyword, str);
                        fileAsString += str;
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
                            if (!(entry = FormatChecker.basicBibTeXCheck(bibBuilder.toString())).equals("invalid")) {

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
     * @param selectedItem item selected from bibList
     * @return selected Bib-Entry
     */
    public String getBibEntry(TableEntry selectedItem) {
        if (bibMap.isEmpty()) {
            return "Cant edit empty file!";
        } else {
            String entry = bibMap.get(selectedItem.getKeyword());
            if (entry == null) {
                return "Error could not find selected entry!";
            }
            return FormatChecker.basicBibTeXCheck(entry);
        }
    }
}