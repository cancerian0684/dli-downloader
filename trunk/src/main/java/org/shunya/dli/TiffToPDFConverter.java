package org.shunya.dli;

import com.itextpdf.text.*;
import com.itextpdf.text.io.RandomAccessSource;
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.BarcodeEAN;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.codec.TiffImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class TiffToPDFConverter {
    final Logger logger = LoggerFactory.getLogger(TiffToPDFConverter.class);

    public int convert(String rootDir, String barcode, String pdfName, AppContext appContext, Downloader downloader, String rootUrl, DLIDownloader adminData, Rectangle pageSize, LogWindow logWindow) throws IOException, DocumentException, CancelledExecutionException {
        int failures = 0;
        if (appContext.isShutdown()) {
            logger.info("Conversion cancelled due to shutdown in progress.");
            logWindow.log("Conversion cancelled due to shutdown in progress.");
            return failures;
        }
        Path directory = Paths.get(rootDir, barcode);
        logger.info("Converting directory to PDF " + directory);
        logWindow.log("Converting directory to PDF " + directory);
        List<Path> allTiffFiles = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory, entry -> entry.getFileName().toString().toLowerCase().endsWith(".tif") || entry.getFileName().toString().toLowerCase().endsWith(".tiff"))) {
            for (Path path : ds) {allTiffFiles.add(path);}
            Collections.sort(allTiffFiles, (first, second) -> first.getFileName().compareTo(second.getFileName()));

            Document document = new Document(pageSize);
            OutputStream os = Files.newOutputStream(Paths.get(rootDir, pdfName));
//            ByteArrayOutputStream outfile = new ByteArrayOutputStream();        //write the contents to byte array first and at the end of the conversion change it to file.
            PdfWriter pdfWriter = PdfWriter.getInstance(document, os);
            pdfWriter.setStrictImageSequence(true);
            document.open();
            if (appContext.isCreateBarcodePage()) {
                logWindow.log("Adding metadata to the output PDF");
                try {
                    addMetaData(barcode, adminData, document, pdfWriter, pageSize);
                    logWindow.log("Added metadata page..");
                } catch (Exception e) {
                    logWindow.log("Exception adding metadata to page.." + e.getMessage());
                }

            } else {
                logWindow.log("Skipping addition of Metadata Page.");
            }
            int count = 0;
            List<String> failedTiffs = new ArrayList<>();
            for (Path path : allTiffFiles) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new CancelledExecutionException("Execution Cancelled, quiting PDF conversion");
                }
                int failureCount = 0;
                AtomicBoolean status = new AtomicBoolean(false);
                while (!addTiffToDocument(document, path, pageSize, logWindow, status) && failureCount < appContext.getMaxConsecutiveIOFailure()) {
                    try {
                        ++failureCount;
                        logger.info("pdf conversion failed, retrying download for tiff : " + path);
                        logWindow.log("pdf conversion failed, retrying download for tiff : " + path);
                        downloader.download(rootUrl, path.getFileName().toString(), directory.toAbsolutePath().toString(), appContext, true, logWindow, 1 << 23, true); //retry once again overwriting the failed image
                    } catch (Exception e) {
                        logWindow.log("retry download failed for tiff : " + path);
                        logger.info("retry download failed for tiff : " + path);
                        appContext.getTap().pauseIfDisconnected();
                    }
                }
                if(!status.get()){
                    failedTiffs.add(path.toString());
                }
                count++;
            }
            document.close();
            os.close();
            pdfWriter.close();
            logger.warn("No. of Failed TIFF Conversions : " + failedTiffs);
            logWindow.log("No. of Failed TIFF Conversions : " + failedTiffs);
            logger.info("Tiff Images [" + count + "] Converted to PDF, PDF Generated : " + pdfName);
            logWindow.log("Tiff Images [" + count + "] Converted to PDF, PDF Generated : " + pdfName);
        }
        return failures;
    }

    private void addMetaData(String barcode, DLIDownloader adminData, Document document, PdfWriter pdfWriter, Rectangle pageSize) throws DocumentException {
        document.addTitle(adminData.getAttr(AppConstants.Title));
        document.addAuthor(adminData.getAttr(AppConstants.Author));
        document.addLanguage(adminData.getAttr(AppConstants.Language));
        document.addSubject(adminData.getAttr(AppConstants.Subject));
        document.addCreator("DLI-Downloader Tool");
        document.addHeader("Barcode", adminData.getAttr(AppConstants.BARCODE));
        document.addCreationDate();
        document.setPageSize(pageSize);
        document.setMargins(1, 1, 1, 1);
        PdfContentByte cb = pdfWriter.getDirectContent();
        document.add(new Paragraph("Barcode - " + barcode + "\nTitle - " + adminData.getAttr(AppConstants.Title) + "\nSubject - " +
                adminData.getAttr(AppConstants.Subject) + "\nAuthor - " + adminData.getAttr(AppConstants.Author) + "\nLanguage - " +
                adminData.getAttr(AppConstants.Language) + "\nPages - " + adminData.getAttr(AppConstants.TotalPages) +
                "\nPublication Year - " + adminData.getAttr(AppConstants.Year) + "\nCreator - Fast DLI Downloader" +
                "\nhttps://github.com/cancerian0684/dli-downloader" + "\nBarcode EAN.UCC-13 \n"));
        BarcodeEAN codeEAN = new BarcodeEAN();
        codeEAN.setCode(barcode);
        document.add(codeEAN.createImageWithBarcode(cb, null, null));
        codeEAN.setGuardBars(true);
        document.newPage();
    }

    private boolean addTiffToDocument(Document document, Path path, Rectangle pageSize, LogWindow logWindow, AtomicBoolean status) {
        RandomAccessSourceFactory factory = new RandomAccessSourceFactory();
        RandomAccessSource bestSource = null;
        RandomAccessFileOrArray myTiffFile = null;
        try {
            bestSource = factory.createBestSource(path.toAbsolutePath().toString());
            myTiffFile = new RandomAccessFileOrArray(bestSource);
//                int numberOfPages = TiffImage.getNumberOfPages(myTiffFile);
            Image tiff = TiffImage.getTiffImage(myTiffFile, 1);
//            float width = tiff.getWidth() > PageSize.A2.getWidth() ? PageSize.A2.getWidth() : tiff.getWidth();
//            float height = tiff.getHeight() > PageSize.A2.getHeight() ? PageSize.A2.getHeight() : tiff.getHeight();
//            tiff.scaleToFit(width, height);
            tiff.scaleToFit(pageSize.getWidth(), pageSize.getHeight());
            document.add(tiff);
            document.newPage();
            status.set(true);
            return true;
        } catch (Exception e) {
            logger.info("Error converting this TIFF image to Pdf : " + path.getFileName());
            logWindow.log("Error converting this TIFF image to Pdf : " + path.getFileName());
            e.printStackTrace();
        } finally {
            close1(myTiffFile);
            close2(bestSource);
        }
        return false;
    }

    private static void close2(RandomAccessSource bestSource) {
        if (bestSource != null) {
            try {
                bestSource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void close1(RandomAccessFileOrArray myTiffFile) {
        if (myTiffFile != null) {
            try {
                myTiffFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}