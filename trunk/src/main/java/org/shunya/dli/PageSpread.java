package org.shunya.dli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;

public class PageSpread {
    final Logger logger = LoggerFactory.getLogger(PageSpread.class);

    public static class Page {
        final Logger logger = LoggerFactory.getLogger(Page.class);
        final String filename;
        volatile int downloadCount = 0;

        Page(String filename) {
            this.filename = filename;
        }

        public String getAndIncrement() {
            ++downloadCount;
            return filename;
        }

        public String getFilename() {
            return filename;
        }

        public boolean canDownload(int maxCount) {
            if (downloadCount >= maxCount) {
                logger.warn("max download retry count exceed {} for page {}, quiting.. ", downloadCount, filename);
            }
            return downloadCount < maxCount;
        }

        @Override
        public String toString() {
            return filename;
        }
    }

    private final char[] filename = {'0', '0', '0', '0', '0', '0', '0', '0', '.', 't', 'i', 'f'};
    private final Queue<Page> pageQueue = new LinkedList<>();
    private int totalPages;
    private int counter;

    /**
     * Constructor calculates the spread of pages for the given input start page and end page.
     * @param start
     * @param end
     */
    public PageSpread(int start, int end) {
        int currentPage = start;
        while (currentPage >= start && currentPage <= end) {
            char[] seq = String.valueOf(currentPage++).toCharArray();
            int length = seq.length;
            int i = 0;
            for (int j = 8 - length; j < 8; j++) {
                filename[j] = seq[i++];
            }
            Page page = new Page(String.valueOf(filename));
            pageQueue.offer(page);
            totalPages++;
        }
    }

    /**
     * Multiple threads can use this method to poll their next job.
     * This method is non-blocking and returns null if no element is left in the queue.
     * @return
     */
    public synchronized Page poll() {
        final Page page = pageQueue.poll();
        if (page != null)
            ++counter;
        return page;
    }

    /**
     * This method should be used to put back a page if the download has failed by some reason.
     * @param page
     */
    public synchronized void offer(Page page) {
        pageQueue.offer(page);
    }

    public int getTotalPages() {
        return totalPages;
    }

    public synchronized int getCounter() {
        return counter;
    }
}
