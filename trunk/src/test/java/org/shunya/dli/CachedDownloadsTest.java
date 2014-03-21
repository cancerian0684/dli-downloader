package org.shunya.dli;

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

public class CachedDownloadsTest {
    final BarcodeExtractor extractor = new BarcodeExtractor();
    private AppContext appContext = Mockito.mock(AppContext.class, RETURNS_DEEP_STUBS);

    @Test
    public void testCheckPresent() throws Exception {
        CachedDownloads cachedDownloads = new CachedDownloads("D:\\workspace\\DLIDownloader 2; d:\\dli 2", extractor, appContext);
        cachedDownloads.buildLocalIndex();
    }
    @Test
    public void testCheckPresentForBlankInput() throws Exception {
        CachedDownloads cachedDownloads = new CachedDownloads("", extractor, appContext);
        cachedDownloads.buildLocalIndex();
    }

    @Test
    public void testCheckPresentForNullInput() throws Exception {
        CachedDownloads cachedDownloads = new CachedDownloads(null, extractor, appContext);
        cachedDownloads.buildLocalIndex();
    }

    @Test
    public void testCheckPresentForWrongInput() throws Exception {
        CachedDownloads cachedDownloads = new CachedDownloads("null", extractor, appContext);
        cachedDownloads.buildLocalIndex();
    }

}
