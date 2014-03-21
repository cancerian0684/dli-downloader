package org.shunya.dli;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;

public class ExtractHref {
    public void extract() throws IOException {
        Document doc = Jsoup.connect("http://archive.org/details/RajivDixitBharatNirmaanAbhiyaan").timeout(60 * 1000).userAgent("Mozilla").get();
        Elements elements = doc.select("a");
        for (Element link : elements) {
            String value = link.getElementsByAttribute("href").select("a").attr("href");
            String fileSize = link.parent().select("a").get(0).text().replaceAll("[.?:]", "") + ".mp3";
            String fileName = value.substring( value.lastIndexOf('/')+1);
            if (value.endsWith(".mp3")) {
                System.out.println("do you want to download this : " + value);
                int sWhatever = JOptionPane.showConfirmDialog(new JFrame(), ""+value+"\n"+fileSize);
                if (sWhatever == JOptionPane.OK_OPTION) {
                    System.out.println("value = " + value + " / " + fileName);
                    download("http://archive.org" + value, fileName, "E:\\rajiv-dixit");
                }
            }
        }
    }

    public void extractJPG() throws IOException {
        Document doc = Jsoup.connect("http://vediccowproducts.com/content/18-breeds-of-desi-cows-and-bulls").timeout(60 * 1000).userAgent("Mozilla").get();
        Elements elements = doc.select("img");
        int i=0;
        for (Element link : elements) {
            final String attr = link.getElementsByAttribute("src").attr("src");
            if(attr.endsWith(".jpg")){

                System.out.println("do you want to download this : " + attr);
                int sWhatever = JOptionPane.showConfirmDialog(new JFrame(), ""+attr+"\n");
                if (sWhatever == JOptionPane.OK_OPTION) {
                    i++;
                    System.out.println("value = " + attr + " / ");
                    download(attr, i+".jpg", "E:\\rajiv-dixit");
                }
            }
         /*   String value = link.getElementsByAttribute("href").select("a").attr("href");
            String fileSize = link.parent().select("a").get(0).text().replaceAll("[.?:]", "") + ".mp3";
            String fileName = value.substring( value.lastIndexOf('/')+1);
            if (value.endsWith(".jpg")) {

            }*/
        }
    }

    public void extractDLSHQ() throws IOException {
        Document doc = Jsoup.connect("http://www.dlshq.org/download/download.htm#saintsivananda").timeout(60 * 1000).userAgent("Mozilla").get();
        Elements elements = doc.select("a");
        for (Element link : elements) {
            String value = link.getElementsByAttribute("href").select("a").attr("href");
            String fileSize = link.parent().select("a").get(0).text().replaceAll("[.?:]", "") + ".mp3";
            String fileName = value.substring( value.lastIndexOf('/')+1);
            if (value.endsWith(".pdf")) {
                System.out.println("do you want to download this : " + value);
                int sWhatever = JOptionPane.showConfirmDialog(new JFrame(), ""+value+"\n"+fileSize);
                if (sWhatever == JOptionPane.OK_OPTION) {
                    System.out.println("value = " + value + " / " + fileName);
                    download("http://www.dlshq.org/download/" + value, fileName, "E:\\sivananda");
                }
            }
        }
    }


    public static void main(String[] args) throws IOException {
        ExtractHref test = new ExtractHref();
        test.extractDLSHQ();
    }

    public void download(String rootUrl, String fileName, String outputDir) throws IOException {
        long totalBytesRead = 0L;
        Path path = Paths.get(outputDir, fileName);
        HttpURLConnection con = (HttpURLConnection) new URL(rootUrl).openConnection();
        con.setReadTimeout(60 * 1000);
        con.setConnectTimeout(60 * 1000);
        try (FileChannel fileChannel = FileChannel.open(path, EnumSet.of(CREATE_NEW, WRITE));
             ReadableByteChannel rbc = Channels.newChannel(con.getInputStream());) {
            totalBytesRead = fileChannel.transferFrom(rbc, 0, 1 << 28);   // download file with max size 4MB
            System.out.println(fileName + " [ " + totalBytesRead / 1024 + " KB]");
        } catch (FileNotFoundException | MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
