package org.shunya.dli;

import org.junit.Ignore;
import org.junit.Test;

public class DownloadCatalogueTest {
    private DownloadCatalogue catalogue = new DownloadCatalogue();

    @Ignore
    @Test
    public void testFetch() throws Exception {
        LuceneIndexer indexer = new LuceneIndexer(new AppContext());
        try {
            catalogue.fetch(indexer, "Hindi");
        } finally {
            System.out.println("Closing the Index");
            indexer.close();
        }
    }
}
