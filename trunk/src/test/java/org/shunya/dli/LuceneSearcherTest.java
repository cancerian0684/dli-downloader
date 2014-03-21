package org.shunya.dli;

import org.apache.lucene.queryparser.classic.ParseException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class LuceneSearcherTest {

    @Test
    @Ignore
    public void testSearch() throws Exception {
        LuceneSearcher searcher = new LuceneSearcher(new AppContext());
        searcher.search("\"hindi sahit\"~5", 20);
//        searcher.findSimilar("Gita Ki Sampati Aur Sardha , Author = Swami Ramsukh Das", 30);
        searcher.close();
    }

    @Test
    public void testSearchBarcode() throws IOException, ParseException {
        LuceneSearcher searcher = new LuceneSearcher(new AppContext());
        Publication publication = searcher.searchBook("5990010044699");
        System.out.println("publication = " + publication.getTitle());
//        searcher.findSimilar("Gita Ki Sampati Aur Sardha , Author = Swami Ramsukh Das", 30);
        searcher.close();
    }


}
