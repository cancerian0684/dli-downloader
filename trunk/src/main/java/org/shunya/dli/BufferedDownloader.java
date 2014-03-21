package org.shunya.dli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class BufferedDownloader implements Downloader {
    protected static final int BUFFER_SIZE = 4 * 1024;
    final Logger logger = LoggerFactory.getLogger(BufferedDownloader.class);
    private ProgressListener progressListener;

    @Override
    public boolean download(String rootUrl, String fileName, String outputDir, AppContext appContext, boolean overwrite, LogWindow simpleLogger, int maxDownloadSize, boolean tapSpeed) throws CancelledExecutionException {
        Path path = Paths.get(outputDir, fileName);
        try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(path, StandardOpenOption.CREATE_NEW), 1024);) {
            URL url = new URL(rootUrl + fileName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(180000);
            con.setConnectTimeout(100000);
            BufferedInputStream bis = new BufferedInputStream(con.getInputStream());
            byte data[] = new byte[BUFFER_SIZE];  // reading 4KB block at a time
            int bytesRead;
            long totalBytesRead = 0;
            long expectedLength = con.getContentLength();
            simpleLogger.log("Length of the Content = " + expectedLength);
            while ((bytesRead = bis.read(data, 0, BUFFER_SIZE)) >= 0) {
                bos.write(data, 0, bytesRead);
                totalBytesRead += bytesRead;
                if (progressListener != null) {
                    int progress = expectedLength <= 0 ? 0 : (int) (100 * totalBytesRead / expectedLength);
                    progressListener.updateProgress(progress);
                    if (progressListener.isCancelled()) {
                        throw new CancelledExecutionException("Download Cancelled by User");
                    }
                }
            }
            bis.close();
            logger.info(fileName + " [" + totalBytesRead / 1000 + " KB]");
            simpleLogger.log(fileName + " [" + totalBytesRead / 1000 + " KB]");
            return true;
        } catch (MalformedInputException e) {
            e.printStackTrace();
            simpleLogger.log(Utils.getException(e));
        } catch (IOException e) {
            e.printStackTrace();
            simpleLogger.log(Utils.getException(e));
        }
        logger.info("Failed : " + fileName);
        simpleLogger.log("Failed : " + fileName);
        return false;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

}