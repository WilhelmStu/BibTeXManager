package org.wst;

import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.scene.input.Clipboard;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;


public class ClipboardService extends ScheduledService<String> {

    private final Clipboard clipboard = Clipboard.getSystemClipboard();

    @Override
    protected Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {

                System.out.println("test");

                final FutureTask query = new FutureTask(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        return clipboard.getString();
                    }
                });

              Platform.runLater(query);
                return (String) query.get();
            }
        };
    }
}
