package org.shunya.dli;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class SingleInstanceFileLock {
    private File file;
    private FileChannel channel;
    private FileLock lock;
    public final String RING_ON_REQUEST_LOCK;
    private final JFrame jFrame;

    public SingleInstanceFileLock(final String lockFile, JFrame jFrame) {
        RING_ON_REQUEST_LOCK = lockFile;
        this.jFrame = jFrame;
    }

    public boolean checkIfAlreadyRunning() {
        try {
            Path path = FileSystems.getDefault().getPath(System.getProperty("user.home"));
            file = new File(path.resolve(RING_ON_REQUEST_LOCK).toUri());
            if (file.exists()) {
                file.delete();
            }
            channel = new RandomAccessFile(file, "rw").getChannel();
            lock = channel.tryLock();
            if (lock == null) {
                channel.close();
                JOptionPane.showMessageDialog(jFrame, "Only 1 instance of DLI-Downloader can run.");
                System.exit(1);
            }
            file.deleteOnExit();
            // Add shutdown hook to release lock when application shutdown
//            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
            System.out.println("Running");
        } catch (IOException e) {
//            throw new RuntimeException("Could not start process.", e);
            return true;
        }
        return false;
    }

    public void unlockFile() {
        try {
            System.out.println("Releasing the Single Instance Lock");
            if (lock != null) {
                lock.release();
                channel.close();
                file.delete();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class ShutdownHook extends Thread {
        public void run() {
            unlockFile();
        }
    }
}