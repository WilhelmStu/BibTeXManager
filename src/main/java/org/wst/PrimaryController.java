package org.wst;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

public class PrimaryController {

    public TextArea textArea1;

    public PrimaryController() {
    }

    @FXML
    public void initialize() {
        ClipboardService service = new ClipboardService();

        // setup service to check clipboard every second
        service.setPeriod(Duration.seconds(1));
        service.setOnSucceeded(t -> {
            // get string from clipboard
            if (t.getSource().getValue() != null) {
                System.out.println(t.getSource().getValue());
                textArea1.setText(t.getSource().getValue().toString());

                /* maybe used later
                Window st = textArea1.getScene().getWindow();
                st.requestFocus();
                 */
                App.toFront();

            } else {
                textArea1.setText("Empty Clipboard!");
            }
        });
        service.start();
    }

    @FXML
    private void switchToSecondary() throws IOException {
        App.setRoot("secondary");
    }

}
