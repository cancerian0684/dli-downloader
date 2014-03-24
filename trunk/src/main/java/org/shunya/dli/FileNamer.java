package org.shunya.dli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.FileVisitResult.CONTINUE;

public class FileNamer {
    final Logger logger = LoggerFactory.getLogger(FileNamer.class);
    final BarcodeExtractor extractor;
    private final AppContext appContext;
    private Set<String> directories = new HashSet<>();


    public FileNamer(String directories, BarcodeExtractor extractor, AppContext appContext) {
        this.extractor = extractor;
        this.appContext = appContext;
        if (directories != null) {
            String[] split = directories.split("[;,]");
            for (String dir : split) {
                if (dir != null && !dir.isEmpty())
                    this.directories.add(dir);
            }
        }
    }

    public synchronized void renameAllFiles() {
        for (String dir : directories) {
            try {
                if (dir != null) {
                    try {
                        dir = dir.trim();
                        if (!dir.isEmpty() && Files.exists(Paths.get(dir))) {
                            logger.info("Scanning directory [{}] for barcodes - ", dir);
                            AtomicInteger counter = new AtomicInteger(0);
                            Files.walkFileTree(Paths.get(dir), new SimpleFileVisitor<Path>() {
                                @Override
                                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                                    final String filename = path.getFileName().toString();
                                    if (filename.endsWith(".pdf")) {
                                        final String barcode = extractor.extractFromPdf(filename);
                                        final Publication book = appContext.getSearcher().searchBook(barcode);
                                        if (book != null) {
                                            String newFileName = book.toString().replaceAll(appContext.getCleanupRegex(), appContext.getCleanupChar()) + ".pdf";
                                            if (!filename.equalsIgnoreCase(newFileName)) {
                                                path.toFile().renameTo(new File(path.toFile().getParentFile(), newFileName));
                                                counter.incrementAndGet();
                                            }
                                        }
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
    }
}
