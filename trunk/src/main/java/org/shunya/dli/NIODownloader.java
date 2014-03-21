package org.shunya.dli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.*;

public class NIODownloader implements Downloader {
    final AtomicInteger consecutiveIOFailures = new AtomicInteger();
    final AtomicInteger consecutive404Failures = new AtomicInteger();
    final Logger logger = LoggerFactory.getLogger(NIODownloader.class);

    @Override
    public boolean download(String rootUrl, String fileName, String outputDir, AppContext appContext, boolean overwrite, LogWindow logWindow, int maxDownloadSize, boolean tapSpeed) throws CancelledExecutionException {
        Path path = Paths.get(outputDir, fileName);
        if (Files.exists(path) && !overwrite) {
            logger.info(fileName + " Ignoring, File already exists!!");
            return true;
        }
        FileChannel fileChannel = null;
        ReadableByteChannel rbc = null;
        long totalBytesRead;
        try {
            HttpURLConnection con = (HttpURLConnection) new URL(rootUrl + fileName).openConnection();
            con.setReadTimeout(appContext.getReadTimeOutMs());
            con.setConnectTimeout(appContext.getConnTimeOutMs());
            con.setRequestProperty("Referer", "http://dli.gov.in/scripts/scroller.htm");
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/30.0.1599.101 Safari/537.36");
            rbc = Channels.newChannel(con.getInputStream());
            if (!overwrite) {
                fileChannel = FileChannel.open(path, EnumSet.of(CREATE_NEW, WRITE));
            } else {
                fileChannel = FileChannel.open(path, EnumSet.of(CREATE, WRITE));
            }
            totalBytesRead = fileChannel.transferFrom(rbc, 0, maxDownloadSize);   // download file with max size 4MB
            fileChannel.close();
            rbc.close();
            logger.info(rootUrl + fileName + " [ " + totalBytesRead / 1024 + " KB]");
            logWindow.log(rootUrl + fileName + " [ " + totalBytesRead / 1024 + " KB]");
            appContext.incrementBytes(totalBytesRead / 1024);
            consecutiveIOFailures.set(0);
            consecutive404Failures.set(0);
            if (tapSpeed)
                appContext.getBucket().consume(totalBytesRead / 1024);
            return true;
        } catch (FileNotFoundException | MalformedURLException e) {
            logger.error(consecutive404Failures.get() + "- File Not Found on server : " + fileName, e);
            logWindow.log(consecutive404Failures.get() + "- File Not Found on server : " + fileName + " \r\n " + Utils.getException(e));
            if (consecutive404Failures.incrementAndGet() >= appContext.getMaxConsecutive404Failure()) {
                logWindow.log("Exceeded maximum consecutive 404 failures limits [" + appContext.getMaxConsecutive404Failure() + "], quiting download now.");
                throw new RuntimeException("Exceeded maximum consecutive 404 failures limits [" + appContext.getMaxConsecutive404Failure() + "], quiting download now.");
            }
            return true;
        } catch (ClosedByInterruptException e) {
            throw new CancelledExecutionException("NIO Download Cancelled : " + fileName, e);
        } catch (IOException e) {
            logger.error(consecutiveIOFailures.get() + "- IOException downloading the file : " + fileName, e);
            logWindow.log(consecutiveIOFailures.get() + "- IOException downloading the file : " + fileName + "\r\n" + Utils.getException(e));
            appContext.getTap().offAndWaitIfDisconnected();
            if (consecutiveIOFailures.incrementAndGet() >= appContext.getMaxConsecutiveIOFailure()) {
                logWindow.log("Exceeded maximum consecutive IO failures limits [" + appContext.getMaxConsecutiveIOFailure() + "], quiting download now.");
                throw new RuntimeException("Exceeded maximum consecutive IO failures limits [" + appContext.getMaxConsecutiveIOFailure() + "], quiting download now.");
            }
        } catch (InterruptedException e) {
            logger.info("NIO Thread Interrupted : " + fileName);
            Thread.currentThread().interrupt();
            throw new CancelledExecutionException("NIO Download Cancelled : " + fileName);
        } finally {
            close(fileChannel);
            close(rbc);
            if (Thread.currentThread().isInterrupted()) {
                logger.info("NIO Thread Interrupted : " + fileName);
                try {
                    logWindow.log("Deleting possibly corrupt download file : " + path);
                    logger.warn("Deleting possibly corrupt download file : " + path);
                    Files.deleteIfExists(path);
                } catch (IOException ee) {
                    ee.printStackTrace();
                }
            }
            if (appContext.getThreadSleepInMs() > 0) {
                try {
                    TimeUnit.MILLISECONDS.sleep(appContext.getThreadSleepInMs());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new CancelledExecutionException("NIO Download Cancelled : " + fileName);
                }
            }
        }
        return false;
    }

    private void close(Channel channel) {
        if (channel != null && channel.isOpen()) {
            try {
                logger.info("Closing Channel : " + channel.toString());
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int tryGetFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }
}
