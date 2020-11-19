package org.wst.helper;


import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.input.Clipboard;


public class ClipboardService extends ScheduledService<String> {

    private final Clipboard clipboard = Clipboard.getSystemClipboard();
    private String oldClipboard;


    @Override
    protected Task<String> createTask() {

        return new Task<>() {
            @Override
            protected String call() throws Exception {

                while (true) {
                    Task<String> query = new Task<>() {
                        @Override
                        protected String call() {
                            return clipboard.getString();
                        }
                    };
                    Platform.runLater(query);

                    if (oldClipboard == null) {
                        oldClipboard = query.get();
                        return query.get();
                    } else if (!(oldClipboard.equals(query.get()))) {
                        oldClipboard = query.get();
                        return query.get();
                    } else {
                        wait(200);
                    }
                }
            }
        };
    }
}


