package org.shunya.dli;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LogWindow extends JFrame {
    private TextAreaFIFO logArea;

    public LogWindow(int bufferSize, String title) {
        super(title);
        try {
            setIconImage(ImageIO.read(DownloadPanel.class.getResourceAsStream("/images/dli-blue.png")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logArea = new TextAreaFIFO(bufferSize);
        logArea.setRows(30);
        logArea.setColumns(80);
        logArea.setEditable(false);
        logArea.setFont(new Font("Arial Unicode MS", 0, 11));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().add(new JScrollPane(logArea));
        setLocationRelativeTo(null);
        pack();
    }

    public void log(final String line) {
        SwingUtilities.invokeLater(() -> logArea.append(line + "\n"));
    }

    public static void main(String[] args) {
        LogWindow lw = new LogWindow(100, "Log Console");
        lw.setVisible(true);
    }
}
