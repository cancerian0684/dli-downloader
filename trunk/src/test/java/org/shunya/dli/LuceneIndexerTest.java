package org.shunya.dli;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class LuceneIndexerTest {
    private LuceneIndexer indexer;

    @Before
    public void start() throws IOException {
        indexer = new LuceneIndexer(new AppContext());
    }

    @After
    public void stop() {
        indexer.close();
    }

    @Test
    @Ignore
    public void testIndexDocs() throws Exception {
//        indexer.index(new HashMap<String, String>());
    }

    @Test
    @Ignore
    public void optimizeDB() throws IOException, ParseException {
        LuceneSearcher searcher = new LuceneSearcher(new AppContext());
        List<Map<String, String>> publications = searcher.search2("hindi", 0, 50000);
        searcher.close();
        System.out.println("Starting cleanup now..");
        for (Map<String, String> map : publications) {
            indexer.index(map);
        }
        indexer.commit();
    }
}
