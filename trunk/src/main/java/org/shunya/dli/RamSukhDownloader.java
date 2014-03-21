package org.shunya.dli;

public class RamSukhDownloader {

    public void download(String parentUrl, String outputDir, LogWindow logWindow, NIODownloader nioDownloader, AppContext defaultAppContext) throws CancelledExecutionException {
        int chapter = 1;
        int page = 1;
        int failure = 0;
        int counter = 1;
        while (true) {
            String pageUrl = "ch" + chapter + "_" + page + ".jpg";
//            String nextPage = getNextPage(counter++);
            if (Utils.pingUrl(parentUrl + pageUrl, logWindow)) {
                ++page;
                failure = 0;
//                System.out.println(parentUrl + pageUrl);
                nioDownloader.download(parentUrl, pageUrl, outputDir, defaultAppContext, false, logWindow, 1 << 22, false);
            } else {
                if (failure >= 2) {
                    System.exit(0);
                }
                ++chapter;
                ++failure;
            }
        }
    }

    public static void main(String[] args) throws CancelledExecutionException {
        AppContext defaultAppContext = new AppContext();
        defaultAppContext.setSpeedLimitKBps(500);
        NIODownloader nioDownloader = new NIODownloader();
        LogWindow logWindow = new LogWindow(500, "RamSukh");
        logWindow.setVisible(true);
        RamSukhDownloader downloader = new RamSukhDownloader();
//        downloader.download("http://www.swamiramsukhdasji.org/swamijibooks/pustak/pustak1/html/sundar%20samaj%20ka%20nirman/", "E:\\RamSukhDas\\Sundar Samaaj Ka Nirmaan", logWindow, nioDownloader, defaultAppContext);
//        downloader.download("http://www.swamiramsukhdasji.org/swamijibooks/pustak/pustak1/html/klyankariprawachana/", "E:\\RamSukhDas\\klyankariprawachana", logWindow, nioDownloader, defaultAppContext);
        downloader.download("http://www.swamiramsukhdasji.org/swamijibooks/pustak/pustak1/html/sadhansudhasindhu/", "E:\\Dharma\\RamSukhDas\\sadhansudhasindhu", logWindow, nioDownloader, defaultAppContext);
    }

    public String getNextPage(int currentPage) {
        final char[] filename = {'a', 'm', 'r', 'i', 't', '0', '0', '0', '.', 'j', 'p', 'g'};
        char[] seq = String.valueOf(currentPage++).toCharArray();
        int length = seq.length;
        int i = 0;
        for (int j = 8 - length; j < 8; j++) {
            filename[j] = seq[i++];
        }
        return String.valueOf(filename);
    }
}
