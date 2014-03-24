package org.shunya.dli;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.awt.*;
import java.nio.file.FileSystems;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static java.util.Arrays.asList;

@XmlRootElement
public class AppContext {
    @Settings(ignore = true)
    private int logBufferSize = 500;
    @Settings(description = "Max download speed limit in KB per seconds, range 100 to 8000")
    private int speedLimitKBps = 500;
    @Settings(description = "x position of the DLI window", ignore = true)
    private int x = 100;
    @Settings(ignore = true)
    private int y = 100;
    @Settings(ignore = true)
    private int width = 510;
    @Settings(ignore = true)
    private int height = 592;
    @Settings(description = "Maximum number of parallel Jobs, valid values 1-6")
    private int numberParallelJobs = 1;
    @Settings(description = "Maximum amount of time (ms) to wait for server response")
    private int readTimeOutMs = 180 * 1000;
    @Settings(description = "Maximum amount of time (ms) to wait for initial server connection")
    private int connTimeOutMs = 100 * 1000;
    @Settings(description = "Quality of PDF pages, valid values = A2, A3, A4, A5. A2 is better than A5")
    private String quality = "A2";
    private String cleanupRegex = "[:?\"><*\\\\/|]";   //"[^a-zA-Z0-9-_\\.\\s,]"
    private String cleanupChar = "";
    private String rootDirectory = FileSystems.getDefault().getPath(System.getProperty("user.dir")).resolve("DLI").toString();
    @XmlTransient
    @Settings(description = "User Settings directory for DLI downloads", editable = false)
    private String userHome = FileSystems.getDefault().getPath(System.getProperty("user.home")).toString();
    @Settings(description = "Parent location for dli_index folder")
    private String indexLocation = FileSystems.getDefault().getPath(".").toString();
    private transient volatile boolean shutdown = false;
    @Settings(description = "Enables search for extra pages after the end of eBook, if the index has wrong information")
    private boolean searchForExtraPages = true;
    @XmlTransient
    private Tap tap;
    private TokenBucket bucket;
    @Settings(description = "How many times to retry a failed TIFF download")
    private int maxRetryCount = 3;
    @Settings(description = "How many Network IO failures to ignore before quiting the entire Book download")
    private int maxConsecutiveIOFailure = 3;
    @Settings(description = "How many missing pages on server to ignore for a particular book before quiting download")
    private int maxConsecutive404Failure = 30;
    @Settings(description = "Do you want to delete the interim TIFF images folder after successful PDF conversion")
    private boolean deleteTifIfSuccessful = true;
    @Settings(description = "How many failures to ignore for TIFF folder deletion")
    private int maxFailureForTIFFDeletion = 0;
    @Settings(description = "Maximum number of initial pages to scan on the server for checking the presence of an eBook")
    private int maxPagesToScanForServer = 2;
    @Settings(description = "If internet connection fails, then what interval should be used for retrying download")
    private int retryIntervalInSeconds = 30;
    @Settings(description = "Which www url to ping for checking internet connection ?")
    private String urlForInternetConnectionCheck = "http://www.google.co.in";
    @Settings(description = "Which DLI server to use for downloading the Book Metadata ?")
    private String metadataServer = "http://www.dli.gov.in";
    @Settings(description = "Comma separated local directories to scan for existing dli downloads ?")
    private String downloadDirectories = "";
    @Settings(description = "Are local directories already indexed ?")
    private boolean indexedLocalDirectories = false;
    @XmlTransient
    private ExecutorService executorService;
    @XmlTransient
    private ExecutorService jobExecutorService;
    @XmlTransient
    private final ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(10000, true) {
        @Override
        public String toString() {
            return "[" + size() + "]";
        }
    };
    @XmlTransient
    private LuceneSearcher searcher;
    @XmlTransient
    private LuceneIndexer indexer;
    @Settings(description = "Total Pages downloaded", editable = false)
    private volatile long totalPagesDownloaded;
    @Settings(description = "Total Books downloaded", editable = false)
    private volatile long totalBooksDownloaded;
    @Settings(description = "Total bytes downloaded in KB", editable = false)
    private volatile long totalBytesDownloadKB;
    @Settings(description = "This session bytes downloaded in KB", editable = false)
    private volatile long sessionByteDownloadKB;
    @Settings(description = "UI Look & Feel Number, valid values 0 - 4, 0 is system Look")
    private int lookAndFeelNumber = 0; //default is System Look and Feel
    @Settings(description = "Should Tool create a First Page mentioning the metadata of teh book ?")
    private boolean createBarcodePage = true;
    @Settings(description = "Maximum search results to show for the search query")
    private int maxSearchResults = 150;
    private int maxKeyStrokeDelay = 300;
    private transient volatile boolean resetConfig = false;
    @Settings(description = "Comma separated list of DLI servers for download")
    private String dliServers = "http://www.dli.ernet.in,http://www.new1.dli.ernet.in,http://202.41.82.144,http://www.new.dli.ernet.in,http://www.dli.gov.in,http://www.new.dli.gov.in,http://www.dli.gov.in";
    private int threadSleepInMs = 500;
    @XmlTransient
    private String username = System.getProperty("user.name");
    @Settings(description = "User email address for sending failure report")
    private String userEmail;
    private Date lastIndexUpdate;
    private Set<String> indexedLanguages = new HashSet<>(asList("Hindi", "English"));
    @Settings(description = "Automatically refresh index after X days, default is 30 days")
    private int refreshIndexAfterDays = 30;
    @Settings(description = "Automatically Index new Language when a new Book is downloaded ?")
    private boolean automaticallyIndexNewLanguages = true;
//    private String proxyHost;
//    private String proxyPort;
//    private String proxyUser;
//    private String proxyPassword;

    public int getSpeedLimitKBps() {
        return speedLimitKBps;
    }

    @XmlElement
    public void setSpeedLimitKBps(int speedLimitKBps) {
        this.speedLimitKBps = Math.min(speedLimitKBps, 8192);
    }

    public Point getLocation() {
        return new Point(x, y);
    }

    @XmlTransient
    public void setLocation(Point location) {
        this.x = location.x;
        this.y = location.y;
    }

    public String getUserHome() {
        return userHome;
    }

    public int getNumberParallelJobs() {
        return numberParallelJobs;
    }

    @XmlElement
    public void setNumberParallelJobs(int numberParallelJobs) {
        this.numberParallelJobs = Math.min(numberParallelJobs, 6);
    }

    public String getRootDirectory() {
        return rootDirectory;
    }

    @XmlElement
    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    @XmlTransient
    public void setSize(Dimension dimension) {
        this.width = dimension.width;
        this.height = dimension.height;
    }

    public synchronized Tap getTap() {
        if (tap == null) {
            tap = new Tap(this);
        }
        return tap;
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException {
        AppContext appContext = new AppContext();
        Utils.save(AppContext.class, appContext, "dli-settings1.xml");
        AppContext load = Utils.load(AppContext.class, "dli-settings1.xml");
        System.out.println("load = " + load);
    }

    public synchronized TokenBucket getBucket() {
        if (bucket == null)
            bucket = TokenBuckets.newFixedIntervalRefill(1024 * 10, getSpeedLimitKBps(), 1, TimeUnit.SECONDS); //max 10 MB can be asked for by a thread
        return bucket;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    @XmlTransient
    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    public int getReadTimeOutMs() {
        return readTimeOutMs;
    }

    public void setReadTimeOutMs(int readTimeOutMs) {
        this.readTimeOutMs = readTimeOutMs;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    @XmlElement
    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public boolean isDeleteTifIfSuccessful() {
        return deleteTifIfSuccessful;
    }

    @XmlElement
    public void setDeleteTifIfSuccessful(boolean deleteTifIfSuccessful) {
        this.deleteTifIfSuccessful = deleteTifIfSuccessful;
    }

    public int getMaxConsecutiveIOFailure() {
        return maxConsecutiveIOFailure;
    }

    @XmlElement
    public void setMaxConsecutiveIOFailure(int maxConsecutiveIOFailure) {
        this.maxConsecutiveIOFailure = maxConsecutiveIOFailure;
    }

    public int getMaxFailureForTIFFDeletion() {
        return maxFailureForTIFFDeletion;
    }

    @XmlElement
    public void setMaxFailureForTIFFDeletion(int maxFailureForTIFFDeletion) {
        this.maxFailureForTIFFDeletion = maxFailureForTIFFDeletion;
    }

    public String getDownloadDirectories() {
        return downloadDirectories;
    }

    @XmlElement
    public void setDownloadDirectories(String downloadDirectories) {
        this.downloadDirectories = downloadDirectories;
    }

    public int getConnTimeOutMs() {
        return connTimeOutMs;
    }

    @XmlElement
    public void setConnTimeOutMs(int connTimeOutMs) {
        this.connTimeOutMs = connTimeOutMs;
    }

    public synchronized ExecutorService getThreadExecutorService() {
        if (executorService == null) {
            executorService = Executors.newCachedThreadPool();
            System.out.println("Thread ThreadPool Started Successfully.");
        }
        return executorService;
    }

    public synchronized ExecutorService getJobExecutorService() {
        if (jobExecutorService == null) {
            jobExecutorService = new ThreadPoolExecutor(
                    getNumberParallelJobs(), // core thread pool size
                    getNumberParallelJobs(), // maximum thread pool size
                    1, // time to wait before resizing pool
                    TimeUnit.MINUTES,
                    workQueue);
            System.out.println("Job ThreadPool Started Successfully.");
        }
        return jobExecutorService;
    }

    public ArrayBlockingQueue getWorkQueue() {
        return workQueue;
    }

    public String getQuality() {
        return quality;
    }

    @XmlElement
    public void setQuality(String quality) {
        this.quality = quality;
    }

    public long getTotalBytesDownloadKB() {
        return totalBytesDownloadKB;
    }

    @XmlElement
    public void setTotalBytesDownloadKB(long totalBytesDownloadKB) {
        this.totalBytesDownloadKB = totalBytesDownloadKB;
    }

    public long getSessionByteDownloadKB() {
        return sessionByteDownloadKB;
    }

    @XmlTransient
    public void setSessionByteDownloadKB(long sessionByteDownloadKB) {
        this.sessionByteDownloadKB = sessionByteDownloadKB;
    }

    public synchronized void incrementBytes(long KBs) {
        setSessionByteDownloadKB(getSessionByteDownloadKB() + KBs);
        setTotalBytesDownloadKB(getTotalBytesDownloadKB() + KBs);
    }

    public synchronized void incrementPages() {
        setTotalPagesDownloaded(getTotalPagesDownloaded() + 1);
    }

    public synchronized void incrementBooks() {
        setTotalBooksDownloaded(getTotalBooksDownloaded() + 1);
    }

    public int getLookAndFeelNumber() {
        return lookAndFeelNumber;
    }

    @XmlElement
    public void setLookAndFeelNumber(int lookAndFeelNumber) {
        this.lookAndFeelNumber = lookAndFeelNumber;
    }

    public int getLogBufferSize() {
        return logBufferSize;
    }

    @XmlElement
    public void setLogBufferSize(int logBufferSize) {
        this.logBufferSize = logBufferSize;
    }

    public boolean isCreateBarcodePage() {
        return createBarcodePage;
    }

    @XmlElement
    public void setCreateBarcodePage(boolean createBarcodePage) {
        this.createBarcodePage = createBarcodePage;
    }

    public String getCleanupChar() {
        return cleanupChar;
    }

    @XmlElement
    public void setCleanupChar(String cleanupChar) {
        this.cleanupChar = cleanupChar;
    }

    public String getCleanupRegex() {
        return cleanupRegex;
    }

    @XmlElement
    public void setCleanupRegex(String cleanupRegex) {
        this.cleanupRegex = cleanupRegex;
    }

    public boolean isSearchForExtraPages() {
        return searchForExtraPages;
    }

    @XmlElement
    public void setSearchForExtraPages(boolean searchForExtraPages) {
        this.searchForExtraPages = searchForExtraPages;
    }

    public int getMaxSearchResults() {
        return maxSearchResults;
    }

    @XmlElement
    public void setMaxSearchResults(int maxSearchResults) {
        this.maxSearchResults = maxSearchResults;
    }

    public String getIndexLocation() {
        return indexLocation;
    }

    @XmlElement
    public void setIndexLocation(String indexLocation) {
        this.indexLocation = indexLocation;
    }

    @XmlElement
    public void setMaxKeyStrokeDelay(int maxKeyStrokeDelay) {
        if (maxKeyStrokeDelay < 100)
            maxKeyStrokeDelay = 200;
        this.maxKeyStrokeDelay = Math.min(maxKeyStrokeDelay, 700);
    }

    public int getMaxKeyStrokeDelay() {
        return maxKeyStrokeDelay;
    }

    public synchronized LuceneSearcher getSearcher() {
        return searcher;
    }

    @XmlTransient
    public synchronized void setSearcher(LuceneSearcher searcher) {
        this.searcher = searcher;
    }

    public int getMaxConsecutive404Failure() {
        return maxConsecutive404Failure;
    }

    @XmlElement
    public void setMaxConsecutive404Failure(int maxConsecutive404Failure) {
        this.maxConsecutive404Failure = maxConsecutive404Failure;
    }

    public boolean isResetConfig() {
        return resetConfig;
    }

    public void setResetConfig(boolean resetConfig) {
        this.resetConfig = resetConfig;
    }

    public String getDliServers() {
        return dliServers;
    }

    public void setDliServers(String dliServers) {
        this.dliServers = dliServers;
    }

    public int getThreadSleepInMs() {
        return threadSleepInMs;
    }

    @XmlElement
    public void setThreadSleepInMs(int threadSleepInMs) {
        this.threadSleepInMs = threadSleepInMs;
    }

    public int getMaxPagesToScanForServer() {
        return maxPagesToScanForServer;
    }

    @XmlElement
    public void setMaxPagesToScanForServer(int maxPagesToScanForServer) {
        this.maxPagesToScanForServer = maxPagesToScanForServer;
    }

    public LuceneIndexer getIndexer() {
        return indexer;
    }

    @XmlTransient
    public void setIndexer(LuceneIndexer indexer) {
        this.indexer = indexer;
    }

    public int getRetryIntervalInSeconds() {
        return retryIntervalInSeconds;
    }

    public void setRetryIntervalInSeconds(int retryIntervalInSeconds) {
        if (retryIntervalInSeconds < 10)
            retryIntervalInSeconds = 10;
        this.retryIntervalInSeconds = retryIntervalInSeconds;
    }

    public String getUrlForInternetConnectionCheck() {
        return urlForInternetConnectionCheck;
    }

    public void setUrlForInternetConnectionCheck(String urlForInternetConnectionCheck) {
        this.urlForInternetConnectionCheck = urlForInternetConnectionCheck;
    }

    public String getMetadataServer() {
        return metadataServer;
    }

    public void setMetadataServer(String metadataServer) {
        this.metadataServer = metadataServer;
    }

    public boolean isIndexedLocalDirectories() {
        return indexedLocalDirectories;
    }

    public void setIndexedLocalDirectories(boolean indexedLocalDirectories) {
        this.indexedLocalDirectories = indexedLocalDirectories;
    }

    public String getUsername() {
        return username;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public long getTotalPagesDownloaded() {
        return totalPagesDownloaded;
    }

    public void setTotalPagesDownloaded(long totalPagesDownloaded) {
        this.totalPagesDownloaded = totalPagesDownloaded;
    }

    public long getTotalBooksDownloaded() {
        return totalBooksDownloaded;
    }

    public void setTotalBooksDownloaded(long totalBooksDownloaded) {
        this.totalBooksDownloaded = totalBooksDownloaded;
    }

    public Date getLastIndexUpdate() {
        return lastIndexUpdate;
    }

    public void setLastIndexUpdate(Date lastIndexUpdate) {
        this.lastIndexUpdate = lastIndexUpdate;
    }

    public Set<String> getIndexedLanguages() {
        return indexedLanguages;
    }

    public void setIndexedLanguages(Set<String> indexedLanguages) {
        this.indexedLanguages = indexedLanguages;
    }

    public int getRefreshIndexAfterDays() {
        return refreshIndexAfterDays;
    }

    public void setRefreshIndexAfterDays(int refreshIndexAfterDays) {
        this.refreshIndexAfterDays = refreshIndexAfterDays;
    }

    public boolean isAutomaticallyIndexNewLanguages() {
        return automaticallyIndexNewLanguages;
    }

    public void setAutomaticallyIndexNewLanguages(boolean automaticallyIndexNewLanguages) {
        this.automaticallyIndexNewLanguages = automaticallyIndexNewLanguages;
    }
}
