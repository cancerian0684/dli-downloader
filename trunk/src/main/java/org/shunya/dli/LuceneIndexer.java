package org.shunya.dli;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;

public class LuceneIndexer {
    final Logger logger = LoggerFactory.getLogger(LuceneIndexer.class);
    private final AppContext appContext;
    private IndexWriter writer;
    private final Set<String> ignoreList = new HashSet<>(asList("scannerno1", "slocation1", "vendor1", "right1"));

    public LuceneIndexer(AppContext appContext) throws IOException {
        this.appContext = appContext;
        Date start = new Date();
        Directory dir = NIOFSDirectory.open(new File(appContext.getIndexLocation(), AppConstants.DLI_INDEX));
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_46, analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        iwc.setMergePolicy(new TieredMergePolicy());
        iwc.setIndexDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy());
        iwc.setRAMBufferSizeMB(32);
        writer = new IndexWriter(dir, iwc);
        Date end = new Date();
        System.out.println(end.getTime() - start.getTime() + " total milliseconds");
    }

    public void commit() {
        try {
            writer.deleteUnusedFiles();
            writer.maybeMerge();
            writer.commit();
            logger.info("Total Number of Docs : " + writer.numDocs());
        } catch (Exception e) {e.printStackTrace();}
    }

    public void close() {
        try {
            writer.maybeMerge();
            writer.forceMerge(5, true);
            writer.forceMergeDeletes(true);
//            writer.commit();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void index(Map<String, String> metadata) throws IOException {
        if (metadata.containsKey(AppConstants.BARCODE)) {
            Document doc = new Document();
            StringBuilder misc = new StringBuilder(500);
            for (String key : metadata.keySet()) {
                String value = metadata.get(key);
                if (value != null && !value.isEmpty() && !ignoreList.contains(key)) {
                    value = Utils.clean(value);
                    if (!key.equals(AppConstants.FILE_PATH) && !key.equals(AppConstants.URL))
                        misc.append(value).append(" ");
                    if (key.equals(AppConstants.BARCODE) || key.equals(AppConstants.FILE_PATH)) {
                        doc.add(new StringField(key, value, Field.Store.YES));
                    } else {
                        doc.add(new TextField(key, value, Field.Store.YES));
                    }
                }
            }
            doc.add(new TextField("misc", misc.toString(), Field.Store.NO));
            index(doc, metadata.get(AppConstants.BARCODE));
        }
    }

    private void index(Document doc, String barcode) throws IOException {
        logger.debug("Indexing Document : " + barcode);
        if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
            writer.addDocument(doc);
        } else {
            writer.updateDocument(new Term(AppConstants.BARCODE, barcode), doc);
        }
    }

    public IndexWriter getWriter() {
        return writer;
    }
}