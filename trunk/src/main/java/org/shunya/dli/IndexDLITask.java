package org.shunya.dli;

import com.itextpdf.text.DocumentException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IndexDLITask implements InteractiveTask {
    private final String language;
    private final LuceneIndexer indexer;
    int perPage = 500;
    volatile int start = 0;
    int end = 200000;
    private final AtomicInteger current = new AtomicInteger(0);
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
    public void cancel() {
        if (state == RunState.Completed)
            return;
        cancel = true;
        state = RunState.Cancelling;
        notifyObserver();
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
        state = RunState.Downloading;
        notifyObserver();
        current.set(start);
        Document doc = Jsoup.connect(appContext.getMetadataServer() + "/cgi-bin/advsearch_db.cgi?listStart=" + 0 + "&perPage=" + 10 + "&language1=" + language + "&scentre=Any&search=Search").timeout(2 * 60000).userAgent("Mozilla").get();
        try {
            end = Integer.parseInt(doc.select("table tbody tr td b").get(1).text());
            logWindow.log("Total Number of Books for Language " + language + " - " + end);
        } catch (Exception e) {
            logWindow.log("Error finding the total number of pages, defaulting to 2 lac.");
        }
        notifyObserver();
        final AtomicBoolean continueLoop = new AtomicBoolean(true);
        List<Future> delayed = new ArrayList<>();
        while (!cancel && continueLoop.get() && (start <= end)) {
            System.out.println("start.get() = " + start);
            final int startCount = start;
            delayed.add(appContext.getThreadExecutorService().submit(() -> {
                downloadPage(continueLoop, startCount, perPage, end);
                return null;
            }));
            if (delayed.size() > Runtime.getRuntime().availableProcessors()) {
                delayed.forEach(e -> {
                    try {
                        e.get();
                    } catch (Exception ex) {
                        logWindow.log(Utils.getException(ex));
                        ex.printStackTrace();
                        throw new RuntimeException(ex);
                    }
                });
                delayed.clear();
            }
            start += perPage;
        }
        delayed.forEach(e -> {
            try {
                e.get();
            } catch (Exception ex) {
                logWindow.log(Utils.getException(ex));
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        });
        if (cancel) {
            state = RunState.Cancelled;
            notifyObserver();
            return;
        }
        if (continueLoop.get()) {
            progress = 100;
            state = RunState.Completed;
            notifyObserver();
        }
    }

    public void downloadPage(AtomicBoolean continueLoop, final int start1, final int perPage1, final int end1) {
        try {
            Document doc;
            logWindow.log("start = " + start1 + " , perPage = " + perPage1);
            final String url1 = appContext.getMetadataServer() + "/cgi-bin/advsearch_db.cgi?listStart=" + start1 + "&perPage=" + perPage1 + "&language1=" + language + "&scentre=Any&search=Search";
//                doc = Jsoup.connect(url1).timeout(2 * 60000).userAgent("Mozilla").get();
            doc = Jsoup.parse(new URL(url1).openStream(), "UTF-8", url1);
            Elements links = doc.select("table tbody tr td a");
            for (Element link : links) {
                String result = link.toString();
                try {
                    Map<String, String> urlParameters = Utils.getUrlParameters(result.replaceAll("&amp;", "&"));
                    if (urlParameters.containsKey(AppConstants.BARCODE)) {
//                    System.out.println("urlParameters = " + urlParameters);
                        continueLoop.set(true);
                        indexer.index(urlParameters);
                        progress = end1 <= 0 ? 0 : 100 * current.incrementAndGet() / end1;
                    }
                } catch (Exception e) {
                    continueLoop.set(false);
                    logWindow.log(result);
                    logWindow.log(e.getMessage());
                    System.out.println(result);
                    System.err.println(e.getMessage());
                }
            }
            progress = end1 <= 0 ? 0 : 100 * current.get() / end1;
            indexer.commit();
            appContext.getSearcher().refresh();
            notifyObserver();
        } catch (IOException e) {
            continueLoop.set(false);
            logWindow.log(Utils.getException(e));
            throw new RuntimeException("Error executing dli-index task", e);
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexDLITask that = (IndexDLITask) o;

        if (!language.equals(that.language)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return language.hashCode();
    }
}
