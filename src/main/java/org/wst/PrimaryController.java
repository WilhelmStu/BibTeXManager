package org.wst;

import java.io.IOException;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.util.Duration;

public class PrimaryController  {

    public TextArea textArea1;
    final Clipboard clipboard = Clipboard.getSystemClipboard();

    public PrimaryController() {
    }


    @FXML
    public void initialize() {
        //ClipboardThread task = new ClipboardThread(textArea1.getText());
        ClipboardService service = new ClipboardService();
        //task.addListener(this);


        service.setPeriod(Duration.seconds(1));
        service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {

            @Override
            public void handle(WorkerStateEvent t) {
                System.out.println(t.getSource().getValue());
                textArea1.setText(t.getSource().getValue().toString());
            }
        });


        service.start();

        /*Task<String> task = service.createTask();


        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();


        task.setOnSucceeded((succeededEvent) -> {
                   textArea1.setText(task.getMessage());

                   Thread thread1 = new Thread(service.createTask());
                   thread1.setDaemon(true);
                   thread1.start();
                }
        );

         */
    }

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

}
