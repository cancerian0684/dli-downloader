package org.shunya.dli;

import java.awt.*;

public interface TapListener {
    public void pause();
    public void resume();
    public void displayMsg(String message, TrayIcon.MessageType msgType);
}
