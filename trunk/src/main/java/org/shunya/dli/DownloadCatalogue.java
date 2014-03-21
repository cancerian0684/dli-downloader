package org.shunya.dli;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.Map;

public class DownloadCatalogue {
    public void fetch(LuceneIndexer indexer, String language) throws IOException {
        boolean continueLoop = true;
        int perPage = 500;
        int start = 0;
        int end = 200000;
        int current = start;
        while (continueLoop && (current <= end)) {
            continueLoop = false;
            System.out.println("start = " + start + " , perPage = " + perPage);
            Document doc = Jsoup.connect("http://202.41.82.144/cgi-bin/advsearch_db.cgi?listStart=" + start + "&perPage=" + perPage + "&language1=" + language + "&scentre=Any&search=Search").timeout(2 * 60000).userAgent("Mozilla").get();
            try{
            end=Integer.parseInt(doc.select("table tbody tr td b").get(1).text());
            }catch (Exception e){System.out.println("Error finding the total number of pages, defaulting to 2 lac.");}
            Elements links = doc.select("table tbody tr td a");
            for (Element link : links) {
                String result = link.toString();
                try {
                    Map<String, String> urlParameters = Utils.getUrlParameters(result.replaceAll("&amp;", "&").replaceAll("[\\n\\t]", ""));
                    if (urlParameters.containsKey(AppConstants.BARCODE)) {
                        ++current;
//                    System.out.println("urlParameters = " + urlParameters);
                        continueLoop = true;
                        indexer.index(urlParameters);
                    }
                } catch (Exception e) {
                    System.out.println(result);
                    System.err.println(e.getMessage());
                }
            }
            indexer.commit();
            start += perPage;
        }
    }
}
