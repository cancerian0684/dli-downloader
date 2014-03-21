package org.shunya.dli;

import com.itextpdf.text.DocumentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class IndexDLITask implements InteractiveTask {
    private final String language;
    private final LuceneIndexer indexer;
    int perPage = 500;
    int start = 0;
    int end = 200000;
    int current;
    private boolean cancel = false;
    private RunState state = RunState.Queued;
    private DownloadObserver observer;
    private Future<?> future;
    private final AppContext appContext;
    private LogWindow logWindow;
    private volatile int progress = 0;

    public IndexDLITask(String language, AppContext appContext, LogWindow logWindow) {
        this.language = language;
        this.appContext = appContext;
        this.logWindow = logWindow;
        this.indexer = appContext.getIndexer();
    }

    @Override
    public String getBarcode() {
        return "Indexing - " + language + " {Total Books - " + end + "}";
    }

    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public String getAttr(String attribute) {
        if (attribute.equals(AppConstants.TotalPages)) {
            return "" + end;
        }
        return "";
    }

    @Override
    public int getFailCount() {
        return 0;
    }

    @Override
    public void cancel() {
        cancel = true;
    }

    @Override
    public void awaitTermination() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public RunState getState() {
        return state;
    }

    @Override
    public void setState(RunState state) {
        this.state = state;
    }

    @Override
    public Future<?> getFuture() {
        return future;
    }

    @Override
    public int getPdfFailures() {
        return 0;
    }

    @Override
    public String getPdfName() {
        return "";
    }

    @Override
    public String getRootDirectory() {
        return "";
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void logs() {
        logWindow.setVisible(true);
    }

    @Override
    public void clean() {
        logWindow.dispose();
    }

    @Override
    public void addMetadata(String key, String value) {

    }

    @Override
    public void download() throws InterruptedException, IOException, CancelledExecutionException, ExecutionException, MetadataNotFound, DocumentException {
        try {
            state = RunState.Downloading;
            notifyObserver();
            boolean continueLoop = true;
            current = start;
            Document doc = Jsoup.connect(appContext.getMetadataServer() + "/cgi-bin/advsearch_db.cgi?listStart=" + 0 + "&perPage=" + 10 + "&language1=" + language + "&scentre=Any&search=Search").timeout(2 * 60000).userAgent("Mozilla").get();
            try {
                end = Integer.parseInt(doc.select("table tbody tr td b").get(1).text());
                logWindow.log("Total Number of Books for Language " + language + " - " + end);
            } catch (Exception e) {
                logWindow.log("Error finding the total number of pages, defaulting to 2 lac.");
            }
            notifyObserver();
            while (!cancel && continueLoop && (current <= end)) {
                continueLoop = false;
                logWindow.log("start = " + start + " , perPage = " + perPage);
                final String url1 = appContext.getMetadataServer() + "/cgi-bin/advsearch_db.cgi?listStart=" + start + "&perPage=" + perPage + "&language1=" + language + "&scentre=Any&search=Search";
//                doc = Jsoup.connect(url1).timeout(2 * 60000).userAgent("Mozilla").get();
                doc = Jsoup.parse(new URL(url1).openStream(), "UTF-8", url1);
                Elements links = doc.select("table tbody tr td a");
                for (Element link : links) {
                    String result = link.toString();
                    try {
                        Map<String, String> urlParameters = Utils.getUrlParameters(result.replaceAll("&amp;", "&"));
                        if (urlParameters.containsKey(AppConstants.BARCODE)) {
                            ++current;
//                    System.out.println("urlParameters = " + urlParameters);
                            continueLoop = true;
                            indexer.index(urlParameters);
                            progress = end <= 0 ? 0 : 100 * current / end;
                        }
                    } catch (Exception e) {
                        logWindow.log(result);
                        logWindow.log(e.getMessage());
                        System.out.println(result);
                        System.err.println(e.getMessage());
                    }
                }
                progress = end <= 0 ? 0 : 100 * current / end;
                indexer.commit();
                appContext.getSearcher().refresh();
                start += perPage;
                notifyObserver();
            }
        } finally {
            progress = 100;
            state = RunState.Completed;
            notifyObserver();
        }
    }

    @Override
    public void beforeStart() {
        this.observer.started(this);
    }

    @Override
    public void afterComplete() {
        try {
            this.observer.completed(this);
            logWindow.log("Refreshing the Index Searcher");
            appContext.getSearcher().refresh();
            logWindow.log("Index Refreshed");
        } catch (Exception e) {
            logWindow.log(Utils.getException(e));
        }
    }

    @Override
    public void withObserver(DownloadObserver observer) {
        this.observer = observer;
    }

    @Override
    public void notifyObserver() {
        observer.update(this);
    }

    @Override
    public void setFuture(Future<?> future) {
        this.future = future;
    }
}
