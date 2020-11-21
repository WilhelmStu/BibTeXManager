package org.wst.helper;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private Task<List<File>> getFileSearchTask(File dir) {
        return new Task<>() {
            @Override
            protected List<File> call() {
                return getAllSubFiles(dir, new ArrayList<>());
            }
        };
    }

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

    public String getRootDirectory() {
        return rootDirectory != null ? rootDirectory.getAbsolutePath() : "No root selected";
    }

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

    public void selectFileFromList(String filename) {
        if(filesInsideRoot == null) return;
        for (File f: filesInsideRoot
             ) {
            if(f.getName().equals(filename)) {
                selectedFile = f;
            }
        }
    }

    public String getSelectedFileName() {
        return selectedFile != null ? selectedFile.getName() : "No file";
    }

}
