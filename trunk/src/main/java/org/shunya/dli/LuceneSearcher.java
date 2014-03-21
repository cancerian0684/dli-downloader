package org.shunya.dli;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

public class LuceneSearcher {
    final Logger logger = LoggerFactory.getLogger(LuceneSearcher.class);
    private final String field = "misc";
    private QueryParser parser;
    private FSDirectory directory;
    private SearcherManager searcherManager;
    private Analyzer analyzer;

    public LuceneSearcher(IndexWriter writer) throws IOException, ParseException {
        searcherManager = new SearcherManager(writer, true, null);
        analyzer = new StandardAnalyzer(Version.LUCENE_46, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        parser = new QueryParser(Version.LUCENE_46, field, analyzer);
        parser.setAllowLeadingWildcard(true);
        parser.setAnalyzeRangeTerms(true);
    }


    public LuceneSearcher(AppContext appContext) throws IOException, ParseException {
        directory = NIOFSDirectory.open(new File(appContext.getIndexLocation(), AppConstants.DLI_INDEX));
        searcherManager = new SearcherManager(directory, null);
        analyzer = new StandardAnalyzer(Version.LUCENE_46, StopAnalyzer.ENGLISH_STOP_WORDS_SET);
        parser = new QueryParser(Version.LUCENE_46, field, analyzer);
        parser.setAllowLeadingWildcard(true);
        parser.setAnalyzeRangeTerms(true);
    }

    public void refresh() throws IOException {
        searcherManager.maybeRefresh();
    }

    public List<Publication> search(String queryString, int hitsPerPage) throws ParseException, IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            List<Publication> publications = new ArrayList<>(hitsPerPage);
            long start = System.currentTimeMillis();
            Query query;
            try {
                queryString = queryString.trim().toLowerCase();
                if (queryString.isEmpty() || queryString.equals("*") || queryString.equals("**")) {
                    queryString = "*";
                }
                query = parser.parse(queryString);
            } catch (ParseException e) {
//                e.printStackTrace();
                logger.warn("Problem with query syntax  :" + e.getMessage());
                query = parser.parse(QueryParserUtil.escape(queryString));
            }
            TopDocs results = searcher.search(query, hitsPerPage);
            ScoreDoc[] hits = results.scoreDocs;
            int numTotalHits = results.totalHits;
            long end = System.currentTimeMillis();
            System.out.println(query.toString(field) + ", results =" + numTotalHits + ", Total matching docs= " + hits.length + ",Total Docs = " + searcher.getIndexReader().numDocs() + ", Time: " + (end - start) + "ms");
            Highlighter highlighter = new Highlighter(new SimpleHTMLFormatter("<span style='color:red;'>", "</span>"), new QueryScorer(query));
            highlighter.setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
            highlighter.setTextFragmenter(new SimpleFragmenter(200));
            int maxNumFragmentsRequired = 2;
            String fragmentSeparator = "...";
            for (int i = 0; i < hits.length; i++) {
                final Publication publication = getPublication(searcher.doc(hits[i].doc));
                String result = highlighter.getBestFragments(analyzer.tokenStream("title1", new StringReader(publication.toString())), publication.toString(), maxNumFragmentsRequired, fragmentSeparator);
                if (result != null && !result.trim().isEmpty())
                    publication.setSearchText(result);
                else
                    publication.setSearchText(publication.toString());
                publications.add(publication);
            }
            return publications;
        } catch (InvalidTokenOffsetsException e) {
            e.printStackTrace();
        } finally {
            searcherManager.release(searcher);
        }
        return Collections.emptyList();
    }

    public List<Publication> searchExisting(int maxResults) throws ParseException, IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            Query query = parser.parse(AppConstants.FILE_PATH + ":[* TO *]");
            TopDocs topDocs = searcher.search(query, maxResults);
            ScoreDoc[] hits = topDocs.scoreDocs;
            List<Publication> results = new ArrayList<>(maxResults);
            if (hits.length > 0) {
                for (int i = 0; i < hits.length; i++) {
                    final Publication publication = getPublication(searcher.doc(hits[i].doc));
                    publication.setSearchText(publication.toString());
                    results.add(publication);
                }
                return results;
            }
            return Collections.emptyList();
        } finally {
            searcherManager.release(searcher);
        }
    }

    public Map<String, String> search(String barcode) throws ParseException, IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            logger.debug("searching book = {}", barcode);
            TopDocs topDocs = searcher.search(new TermQuery(new Term(AppConstants.BARCODE, barcode)), 1);
            ScoreDoc[] hits = topDocs.scoreDocs;
            if (hits.length > 0) {
                final Document doc = searcher.doc(hits[0].doc);
                Map<String, String> values = new HashMap<>(20);
                final Iterator<IndexableField> iterator = doc.iterator();
                while (iterator.hasNext()) {
                    final IndexableField next = iterator.next();
                    values.put(next.name(), next.stringValue());
                }
                return values;
            }
            return null;
        } finally {
            searcherManager.release(searcher);
        }
    }

    private Publication getPublication(Document doc) {
        Publication pub = new Publication(doc.get("title1"), doc.get(AppConstants.BARCODE), doc.get("author1"), doc.get("language1"), doc.get("subject1"), doc.get("pages"), doc.get("year"));
        pub.setUrl(doc.get(AppConstants.URL));
        pub.setLocalPath(doc.get(AppConstants.FILE_PATH));
        return pub;
    }

    public List<Map<String, String>> search2(String line, int startPage, int hitsPerPage) throws ParseException, IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            List<Map<String, String>> publications = new ArrayList<>(hitsPerPage);
            Query query = parser.parse(line);
            System.out.println();
            long start = System.currentTimeMillis();
            TopDocs results = searcher.search(query, hitsPerPage + startPage);
            long end = System.currentTimeMillis();
            System.out.println("Searching for: " + query.toString(field) + ", time: " + (end - start) + "ms");
            ScoreDoc[] hits = results.scoreDocs;
            int numTotalHits = results.totalHits;
            System.out.println("results = " + numTotalHits + ", total matching docs " + hits.length + ", total docs =" + searcher.getIndexReader().numDocs());
            final String[] tags = new String[]{"barcode", "author1", "title1", "language1", "pages", "subject1", "year", "url"};
            for (int i = startPage; i < hits.length; i++) {
                Document doc = searcher.doc(hits[i].doc);
                Map<String, String> map = new HashMap<>(10);
                for (String tag : tags) {
                    if (doc.get(tag) != null && !doc.get(tag).isEmpty() && !doc.get(tag).equalsIgnoreCase("null")) {
                        map.put(tag, doc.get(tag));
                    }
                }
                publications.add(map);
            }
            return publications;
        } finally {
            searcherManager.release(searcher);
        }
    }

    public void close() throws IOException {
        if (directory != null)
            directory.close();
    }

    /*private void readAllDocs1() throws IOException {
        for (int i = 0; i < searcher.getIndexReader().maxDoc() - 1; i++) {
            Document doc = searcher.getIndexReader().document(i);
            String docId = doc.get("docId");
//            searcher.doc(i);

            // do something with docId here...
        }
    }

    private void readAllDocs2() throws IOException {
        MatchAllDocsQuery query = new MatchAllDocsQuery();
        searcher.search(query, 100);

    }

    private void readAllDocs3() throws IOException {
        TermQuery query = new TermQuery(new Term("tags", "hybrid"));
//        reader.
        searcher.search(query, 100);
    } */

    public Publication searchBook(String barcode) throws IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            TopDocs topDocs = searcher.search(new TermQuery(new Term("barcode", barcode)), 1);
            ScoreDoc[] hits = topDocs.scoreDocs;
            if (hits.length > 0) {
                return getPublication(searcher.doc(hits[0].doc));
            }
            return null;
        } finally {
            searcherManager.release(searcher);
        }
    }


    public int countDocs() throws IOException {
        IndexSearcher searcher = searcherManager.acquire();
        try {
            return searcher.getIndexReader().numDocs();
        } finally {
            searcherManager.release(searcher);
        }
    }
}
