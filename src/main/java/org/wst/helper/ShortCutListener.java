package org.wst.helper;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.robot.Robot;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;

// https://github.com/kwhat/jnativehook

/**
 * This Listener will listen to all global Keyboard events, so also those outside the
 * app in order to detect the pressed shortcut F1 to select and copy possible bib entries on a website
 */
public class ShortCutListener implements NativeKeyListener {
    final static String OS = System.getProperty("os.name");
    final static boolean IsWindows = OS.contains("Windows");

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {

    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {


        if (NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode()).equals(KeyCode.F1.getName())) {
            System.out.println("Key pressed " + NativeKeyEvent.getKeyText(nativeKeyEvent.getKeyCode()));
            Platform.runLater(() -> {

                Robot r = new Robot();
                if (IsWindows) {
                    r.keyPress(KeyCode.CONTROL);
                    r.keyPress(KeyCode.A);
                    r.keyRelease(KeyCode.A);
                    r.keyPress(KeyCode.C);
                    r.keyRelease(KeyCode.C);
                    r.keyRelease(KeyCode.CONTROL);
                } else {
                    r.keyPress(KeyCode.COMMAND);
                    r.keyPress(KeyCode.A);
                    r.keyRelease(KeyCode.A);
                    r.keyPress(KeyCode.C);
                    r.keyRelease(KeyCode.C);
                    r.keyRelease(KeyCode.COMMAND);
                }
            });
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {

    }
}
