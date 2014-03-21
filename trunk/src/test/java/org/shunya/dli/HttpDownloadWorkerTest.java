package org.shunya.dli;

import org.junit.Test;

import java.util.ArrayList;

import static org.mockito.Mockito.*;

public class HttpDownloadWorkerTest {
    PageSpread pageSpread = mock(PageSpread.class);
    Downloader downloader = mock(Downloader.class);
    DownloadObserver downloadObserver = mock(DownloadObserver.class);
    DLIDownloader dliDownloader = mock(DLIDownloader.class);
    AppContext appContext = mock(AppContext.class, RETURNS_DEEP_STUBS);
    LogWindow logWindow = mock(LogWindow.class);

    @Test
    public void testFor25Pages() throws Exception {
        when(pageSpread.getTotalPages()).thenReturn(25);
        HttpDownloadWorker test = new HttpDownloadWorker(pageSpread, "", "some directory", "test", downloader, new ArrayList<PageSpread.Page>(), downloadObserver, dliDownloader, appContext, logWindow);
        test.call();
        verify(downloadObserver).update(dliDownloader);
    }

    @Test
    public void testFor500Pages() throws InterruptedException, CancelledExecutionException {
        when(pageSpread.getTotalPages()).thenReturn(500);
        HttpDownloadWorker test = new HttpDownloadWorker(pageSpread, "", "some directory", "test", downloader, new ArrayList<PageSpread.Page>(), downloadObserver, dliDownloader, appContext, logWindow);
        test.call();
        verify(downloadObserver).update(dliDownloader);
    }

    @Test
    public void testComplete() throws InterruptedException, CancelledExecutionException {
        pageSpread = new PageSpread(1,25);
        HttpDownloadWorker test = new HttpDownloadWorker(pageSpread, "", "some directory", "test", downloader, new ArrayList<PageSpread.Page>(), downloadObserver, dliDownloader, appContext, logWindow);
        test.call();
        verify(downloadObserver, times(26)).update(dliDownloader);
    }
}
