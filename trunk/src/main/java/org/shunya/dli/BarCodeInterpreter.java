package org.shunya.dli;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class BarCodeInterpreter {
    final Logger logger = LoggerFactory.getLogger(BarCodeInterpreter.class);

    private ServerQueue serverQueue;
    private AppContext appContext;

    public BarCodeInterpreter(ServerQueue serverQueue) {
        this.serverQueue = serverQueue;
    }

    public Map<String, String> collect(String url, String barcode, AppContext appContext, LogWindow logWindow, DLIDownloader dliDownloader) throws IOException, MetadataNotFound, CancelledExecutionException {
        this.appContext = appContext;
        logWindow.log("Fetching metadata for Barcode : " + barcode);
        Map<String, String> adminData = new HashMap<>();
        final Iterator<DLIServer> dliServerIterator = serverQueue.getMetadataServers().iterator();
        while (dliServerIterator.hasNext()) {
            final DLIServer dliServer = dliServerIterator.next();
            dliDownloader.assertNotCancelled();
            final String finalUrl = dliServer.getRootUrl() + url + barcode;
            if (Utils.pingUrl(finalUrl, logWindow)) {
                Document doc = Jsoup.connect(finalUrl).timeout(appContext.getReadTimeOutMs()).userAgent("Mozilla/5.0").get();
                Elements links = doc.select("table tbody tr");
                for (Element link : links) {
                    String rowKey = link.select("td").get(0).text();
                    String rowVal = link.select("td").get(1).text();
                    if (rowKey.equalsIgnoreCase(AppConstants.TotalPages) && rowVal.trim().isEmpty()) {
                        break;
                    }
                    adminData.put(rowKey, rowVal);
                    if (rowKey.equalsIgnoreCase(AppConstants.ReadOnline)) {
                        adminData.put("url", extractUrl(link.getElementsByAttribute("href").select("a").attr("href")));
                    }
                }
                if (adminData.containsKey(AppConstants.TotalPages)) {
                    logger.info("Fetched AdminData = " + adminData);
                    logWindow.log("Fetched AdminData = " + adminData);
                    dliServer.incrementMetadataCount();
                    return adminData;
                } else {
                    dliServer.getMetadataCount().decrementAndGet();
                    logger.warn("Metadata not found on this server : " + dliServer);
                    logWindow.log("Metadata not found on this server : " + dliServer);
                }
            } else {
                dliServer.resetMetadataCount();
                logger.warn("Metadata not found on this server : " + dliServer);
            }
        }
        logWindow.log("Server URL & Metadata not found for the BarCode : " + barcode);
        throw new MetadataNotFound("Server URL & Metadata not found for the BarCode : " + barcode);
    }

    public String extractUrl(String input) {
        input = input.replaceAll("[\r\n]", "");
        StringBuilder output2 = new StringBuilder();
        String substring = input.substring(input.indexOf("=") + 1);
        char[] chars = substring.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char aChar = chars[i];
            if (aChar == '&')
                break;
            output2.append(aChar);
        }
        return output2.toString() + "/PTIFF/";
    }

    public List<String> getValidHost(String postUrl, LogWindow logWindow, DLIDownloader dliDownloadRequest) throws CancelledExecutionException, MetadataNotFound {
        logger.info("Resolving URL to the server.");
        String pages[] = {"00000001.tif", "00000007.tif", "00000013.tif", "00000017.tif"};
        int maxPagesToScan = Math.min(appContext.getMaxPagesToScanForServer(), pages.length);
        List<String> validServers = new ArrayList<>(10);
        serverQueue.getDownloadServers().parallelStream()
                .forEach(server -> {
                    for (int i = 0; i < maxPagesToScan; i++) {
                        dliDownloadRequest.assertNotCancelled();
                        if (Utils.pingUrl(server.getRootUrl() + postUrl + "/" + pages[i], logWindow)) {
                            validServers.add(server.getRootUrl());
                            server.incrementDownloadCount();
                            break;
                        }
                        server.getDownloadCount().decrementAndGet();
                    }
                });
        logger.info("valid Servers Found for Barcode  " + dliDownloadRequest.getBarcode() + ", are = " + validServers);
        logWindow.log("valid Servers Found for Barcode  " + dliDownloadRequest.getBarcode() + ", are = " + validServers);
        if (validServers.size() < 1)
            throw new MetadataNotFound("No valid Server/URL Found for Barcode : " + dliDownloadRequest.getBarcode());
        return validServers;
    }
}
