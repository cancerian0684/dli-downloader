package org.shunya.dli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import static java.nio.file.FileVisitResult.CONTINUE;

public class Utils {
    static final Logger logger = LoggerFactory.getLogger(Utils.class);

    public static int[] getColumnWidth(JTable table) {
        int[] columnWidth = new int[table.getColumnModel().getColumnCount()];
        for (int i = 0; i < columnWidth.length; i++) {
            columnWidth[i] = table.getColumnModel().getColumn(i).getPreferredWidth();
        }
        return columnWidth;
    }

    public static void initializeTableColumns(JTable table, int size[]) {
        TableCellRenderer dcr = table.getDefaultRenderer(Integer.class);
        if (dcr instanceof JLabel) {
            ((JLabel) dcr).setHorizontalAlignment(SwingConstants.CENTER);
        }
        table.setDefaultRenderer(Integer.class, dcr);
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            TableCellRenderer headerRenderer = header.getDefaultRenderer();
            if (headerRenderer instanceof JLabel) {
                ((JLabel) headerRenderer).setHorizontalAlignment(JLabel.CENTER);
            }
            header.setPreferredSize(new Dimension(30, 25));
        }
        TableColumn column;
        for (int i = 0; i < size.length; i++) {
            column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(size[i]);
        }
    }

    public static <T> void save(Class<T> clazz, T obj, String fileName) {
        try {
            Path path = FileSystems.getDefault().getPath(System.getProperty("user.home"));
            File file = new File(path.resolve(fileName).toUri());
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(obj, file);
            logger.info("Persisting File : " + fileName);
            marshaller.marshal(obj, System.out);
        } catch (JAXBException e) {
            System.err.println("Error saving settings file to disk.");
            e.printStackTrace();
        }
    }

    public static <T> T load(Class<T> clazz, String fileName) throws IllegalAccessException, InstantiationException {
        try {
            logger.info("Loading File : " + fileName);
            Path path = FileSystems.getDefault().getPath(System.getProperty("user.home"));
            File file = new File(path.resolve(fileName).toUri());
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            T config = (T) unmarshaller.unmarshal(file);
            return config;
        } catch (JAXBException e) {
            System.err.println("Could not load XML file : " + fileName);
            e.printStackTrace();
        }
        return clazz.newInstance();
    }

    public static Map<String, String> getUrlParameters(String url)
            throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        String query = url.substring(url.indexOf("?") + 1);
        for (String param : query.split("\n\t")) {
            String pair[] = param.split("=");
            if (pair == null || pair.length < 2)
                continue;
            String key = URLDecoder.decode(pair[0].replaceAll("&", ""), "utf-8");
            String value = "";
            if (pair.length > 1) {
                try {
                    value = URLDecoder.decode(pair[1], "utf-8");
                } catch (Exception e) {value = pair[1];}
            }
            if (key != null && !key.trim().isEmpty())
                params.put(key.toLowerCase(), value);
        }
        return params;
    }

    public static void deleteDir(String path, final LogWindow logWindow) {
        final Path dir = Paths.get(path);
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {Files.delete(file);} catch (IOException e) {
                        logWindow.log("Couldn't delete File : " + file.toString() + ", Reason : " + e.getMessage());
                    }
                    return CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    logger.info("Deleting Directory : " + dir);
                    if (exc == null) {
                        Files.delete(dir);
                        return CONTINUE;
                    } else {
                        logWindow.log("Couldn't delete Dir : " + dir.toString() + ", Reason : " + exc.getMessage());
                        throw exc;
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeToFile(String log, Path path) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path.toFile()))) {
            bufferedWriter.write(log);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readFromFile(Path path) {
        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getException(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    public static String[] extractBarcodes(String contents) {
        return contents.split("[\r\n;,:-]");
    }

    public static void listSystemProperties(PrintWriter pw) {
        Properties props = System.getProperties();
        props.list(pw);
    }

    public static boolean pingUrl(final String address, LogWindow logWindow) {
        try {
            final URL url = new URL(address);
            final HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
            urlConn.setRequestMethod("HEAD");
            urlConn.setConnectTimeout(1000 * 20); // mTimeout is in seconds
            urlConn.setReadTimeout(1000 * 20); // mTimeout is in seconds
            final long startTime = System.currentTimeMillis();
            urlConn.connect();
            int responseCode = urlConn.getResponseCode();
            final long endTime = System.currentTimeMillis();
            if (200 <= responseCode && responseCode <= 399) {
                logger.info("Ping to " + address + " was success, " + responseCode + ", Time (ms) : " + (endTime - startTime));
                if (logWindow != null)
                    logWindow.log("Ping to " + address + " was success, " + responseCode + ", Time (ms) : " + (endTime - startTime));
                return true;
            }
            logger.info("Ping to " + address + " was failure, " + responseCode + ", Time (ms) : " + (endTime - startTime));
            if (logWindow != null)
                logWindow.log("Ping to " + address + " was failure, " + responseCode + ", Time (ms) : " + (endTime - startTime));
            urlConn.disconnect();
        } catch (final MalformedURLException e1) {
//            logger.warn("", e1);
        } catch (final IOException e) {
            logger.warn("ping to " + address + " was failure {}", e.getMessage());
            if (logWindow != null)
                logWindow.log(e.toString());
        }
        return false;
    }

    public static String clean(String value) {
        return value.trim().replaceAll("[\"]", "").replaceAll("(?i)(none)", "").replaceAll("(?i)(null)", "").replaceAll(" +", " ").replaceAll("[_]", " ");
    }

    public static String removeDuplicateWords(String input) {
        final String[] split = input.split("[,\\s\n\r]");
        HashSet<String> words = new HashSet<>(100);
        StringBuilder stringBuilder = new StringBuilder();
        for (String word : split) {
            if (words.add(word)) {
                stringBuilder.append(" " + word);
            }
        }
        return stringBuilder.toString();
    }

    public static int tryGetFileSize(URL url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.getInputStream();
            return conn.getContentLength();
        } catch (IOException e) {
            return -1;
        } finally {
            conn.disconnect();
        }
    }

    public static String readContent(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));) {
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line+"\n");
            }
            return out.toString();
        }
    }

    public static boolean checkNotNull(String attr) {return attr != null && !attr.trim().isEmpty() && !attr.trim().equalsIgnoreCase("null")&& !attr.trim().equalsIgnoreCase("-");}
}
