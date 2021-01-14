package org.wst.helper;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Class to handle undo and redo of any file-operations in the app
 * A linkedList is used as a stack where the last 10 moves are stored with
 * the complete file content as string, the File pointer as well as the action performed
 * to get to that state
 */
public class UndoRedoManager {
    private UndoRedoManager() {
        this.history = new LinkedList<>();
        this.pointer = -1;
        this.fileManager = FileManager.getInstance();
    }

    private static final UndoRedoManager undoRedoManager = new UndoRedoManager();

    public static UndoRedoManager getInstance() {
        return undoRedoManager;
    }

    public enum Action {
        INIT,
        DELETE,
        WRITE,
        REFORMAT,
        NONE_LEFT,
        ERROR
    }

    private int pointer;
    private final static int stackSize = 10;
    private final LinkedList<Operation> history;
    private final FileManager fileManager;

    /**
     * Saves an operation to the stack, if the max size of 10 is reached
     * it removes the last item in the list
     * If there was an undo before, and a new operation is saved,
     * all possible redo steps are removed from the list
     *
     * @param fileAsString file content
     * @param file         file pointer
     * @param action       to get to this file content
     */
    public void saveOperation(String fileAsString, File file, Action action) {
        synchronized (fileManager.getLock()) {
            if (pointer == stackSize) {
                history.add(new Operation(action, file, fileAsString));
                history.removeLast();
            } else if (pointer < stackSize) {
                pointer++;
                while (history.size() > pointer) {
                    history.removeFirst();
                }
                history.add(new Operation(action, file, fileAsString));
            }
        }
    }

    /**
     * Undoes the last action, by rewriting the whole file to the old state
     * The undone action is not removed from the stack because it might be
     * redone later
     *
     * @return undone action
     */
    public Action undoLastFileOperation() {
        synchronized (fileManager.getLock()) {
            if (!isUndoPossible()) return Action.NONE_LEFT;
            Operation op = history.get(--pointer);
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(op.getFile().getAbsoluteFile(), false));
                writer.write(op.getFileAsString());
                writer.flush();
                writer.close();

                fileManager.setSelectedFile(op.getFile());
                return history.get(pointer + 1).getAction();

            } catch (IOException e) {
                System.err.println("Error during rewrite of file! (during undo)");
                e.printStackTrace();
                return Action.ERROR;
            }
        }
    }

    /**
     * Redoes the last undone action, if there is an action left to redo
     * If the stack is full (10 Operations) it is not possible to redo anything
     *
     * @return redone action
     */
    public Action redoLastFileOperation() {
        synchronized (fileManager.getLock()) {
            if (++pointer < history.size()) {
                Operation op = history.get(pointer);

                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(op.getFile().getAbsoluteFile(), false));
                    writer.write(op.getFileAsString());
                    writer.flush();
                    writer.close();

                    fileManager.setSelectedFile(op.getFile());
                    return op.getAction();

                } catch (IOException e) {
                    System.err.println("Error during rewrite of file! (during redo)");
                    e.printStackTrace();
                }

            }
            return Action.NONE_LEFT;
        }
    }

    public boolean isUndoPossible() {
        return pointer > 0 && !(history.size() == 1);
    }

    public boolean isRedoPossible() {
        return pointer < stackSize && history.size() > pointer + 1;
    }

    /**
     * First insert, when the stack is empty, cant undo the last item in the stack
     *
     * @return
     */
    public boolean isInit() {
        return history.size() == 0;
    }

    /**
     * Helper class to store operations, each one having an action a file pointer
     * and the file-content as a string
     */
    private static class Operation {
        private final Action action;
        private final File file;
        private final String fileAsString;

        public Operation(Action action, File file, String fileAsString) {
            this.action = action;
            this.file = file;
            this.fileAsString = fileAsString;
        }

        public Action getAction() {
            return action;
        }

        public File getFile() {
            return file;
        }

        public String getFileAsString() {
            return fileAsString;
        }
    }
}

