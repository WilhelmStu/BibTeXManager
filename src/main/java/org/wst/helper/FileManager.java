package org.wst.helper;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private String bibFileAsString;

    /**
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
        String oldText = b.getText();
        b.setDisable(true);
        b.setText("Searching for bib files..");

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
                    fileNames.add(f.getName());
                }

                Collections.sort(fileNames);
                System.out.println(fileNames);
                if (fileNames.size() == 0) fileNames.add("No .bib files found!");
                view.setItems(FXCollections.observableArrayList(fileNames));

                b.setDisable(false);
                b.setText(oldText);

            });
            Thread th = new Thread(t);
            th.start();
        } else {
            b.setDisable(false);
            b.setText(oldText);
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
        String oldText = b.getText();
        b.setDisable(true);
        b.setText("Selecting a bib file..");

        FileChooser fileChooser = new FileChooser();

        if (selectedFile != null) {
            fileChooser.setInitialDirectory(selectedFile.getParentFile());
        }
        fileChooser.setTitle("Select a single file");
        selectedFile = fileChooser.showOpenDialog(b.getScene().getWindow());
        b.setDisable(false);
        b.setText(oldText);
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
            if (f.getName().equals(filename)) {
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

    // todo add check that no duplicates are entered!!!
    /**
     * Will write the given bib-entry into the selected file, validity should be checked before calling this function
     *
     * @param str bib entry to write
     */
    public void writeToFile(String str) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(selectedFile.getAbsoluteFile(), true));
            writer.write("\n");
            writer.write(str);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.err.println("Error writing to file");
            e.printStackTrace();
        }
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
        String oldText = b.getText();
        b.setDisable(true);
        b.setText("Creating a new bib file");

        FileChooser fileChooser = new FileChooser();

        if (selectedFile != null) {
            fileChooser.setInitialDirectory(selectedFile.getParentFile());
        }
        fileChooser.setTitle("Create a new file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("BibTeX Files", "*.bib"));
        fileChooser.setInitialFileName("bibliography.bib");

        File tmp = fileChooser.showSaveDialog(b.getScene().getWindow());
        b.setDisable(false);
        b.setText(oldText);
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
     * Will read the selected file and check for every line if it is a Bib-Entry, will
     * then add valid entries heads in the form "TYPE: keyword" to the list, or an
     * appropriate message if none have been found | an error occurred
     *
     * @return List of Bib-Entries inside the selected file
     */
    public void populateBibList(ListView<String> view) {
        Task<ObservableList<String>> task = new Task<>() {
            @Override
            protected ObservableList<String> call() throws Exception {
                ObservableList<String> entries = FXCollections.observableArrayList();
                if (selectedFile == null) {
                    entries.add("Can't find selected File!");
                    return entries;
                }
                try {
                    FileReader fr = new FileReader(selectedFile);
                    BufferedReader reader = new BufferedReader(fr);
                    String line, tmp;
                    StringBuilder builder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append("\n");
                        if (!(tmp = FormatChecker.getBibEntryHead(line)).equals("invalid")) {
                            entries.add(tmp);
                        }
                    }
                    bibFileAsString = builder.toString();
                } catch (IOException e) {
                    System.err.println("Error reading from file!");
                    e.printStackTrace();
                    entries.add("Error reading from file!");
                }

                Collections.sort(entries);
                if (entries.size() == 0) {
                    entries.add("No entries found!");
                }
                return entries;
            }
        };
        task.setOnSucceeded(list -> {
            view.setItems(task.getValue());
        });

        Thread th = new Thread(task);
        th.start();
    }


    // todo improve this code
    /**
     * Will search the input file for the selected Item and then return
     * the corresponding Bib-Entry, the loop is required, since the searched
     * keyword can be a substring of another entry...
     *
     * @param selectedItem item selected from bibList
     * @return selected Bib-Entry
     */
    public String getBibEntry(String selectedItem) {
        if (bibFileAsString.isEmpty()) {
            return "Cant edit empty file!";
        } else {
            boolean isEqual = false;
            String tmp = bibFileAsString;
            String startOfEntry;
            do {
                String keyword = selectedItem.split(",")[1].trim();
                int startOfBibKey = tmp.indexOf(keyword);
                startOfEntry = tmp.substring(tmp.lastIndexOf("@", startOfBibKey));
                String possibleEntryHead = FormatChecker.getBibEntryHead(startOfEntry);
                if (possibleEntryHead.equals(selectedItem)) break;
                tmp = tmp.substring(startOfBibKey + keyword.length());
            } while (true);
            return FormatChecker.basicBibTeXCheck(startOfEntry);
        }
    }
}