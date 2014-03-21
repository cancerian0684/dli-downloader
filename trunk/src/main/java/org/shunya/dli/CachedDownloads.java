package org.shunya.dli;

import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.FileVisitResult.CONTINUE;

public class CachedDownloads {
    final Logger logger = LoggerFactory.getLogger(CachedDownloads.class);
    final BarcodeExtractor extractor;
    private final AppContext appContext;
    private int commitCacheCunter = 0;

    boolean cacheReady = false;
    private Set<String> directories = new HashSet<>();

    public CachedDownloads(String directories, BarcodeExtractor extractor, AppContext appContext) {
        this.appContext = appContext;
        this.extractor = extractor;
        if (directories != null) {
            String[] split = directories.split("[;,]");
            for (String dir : split) {
                if (dir != null && !dir.isEmpty())
                    this.directories.add(dir);
            }
        }
    }

    public synchronized void awaitCache() throws InterruptedException {
        while (!cacheReady)
            wait();
    }

    public synchronized void buildLocalIndex() {
        for (String dir : directories) {
            try {
                if (dir != null) {
                    try {
                        dir = dir.trim();
                        if (!dir.isEmpty() && Files.exists(Paths.get(dir))) {
                            logger.info("Scanning directory [{}] for barcode - ", dir);
                            AtomicInteger counter = new AtomicInteger(0);
                            Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                                    final String filename = path.getFileName().toString();
                                    if (filename.endsWith(".pdf")) {
                                        add(extractor.extractFromPdf(filename), path);
                                        counter.incrementAndGet();
                                    }
                                    return CONTINUE;
                                }
                            });
                            logger.info("Total files processed from directory [{}] = {}", dir, counter.get());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            commitChanges();
        } catch (IOException e) {
            e.printStackTrace();
        }
        cacheReady = true;
        notifyAll();
    }

    public synchronized void add(String barcode, Path path) {
        final Map<String, String> map;
        try {
            map = appContext.getSearcher().search(barcode);
            if (map != null) {
                map.put(AppConstants.FILE_PATH, path.toFile().getAbsolutePath());
                appContext.getIndexer().index(map);
            }
            commitCacheCunter++;
            if (commitCacheCunter > 500)
                commitChanges();
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void commitChanges() throws IOException {
        appContext.getIndexer().commit();
        appContext.getSearcher().refresh();
        commitCacheCunter = 0;
    }
}
