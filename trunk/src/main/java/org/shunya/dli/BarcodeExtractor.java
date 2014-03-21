package org.shunya.dli;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BarcodeExtractor {
    private final Pattern p = Pattern.compile("\\d+");
    public final static BarcodeExtractor instance = new BarcodeExtractor();

    public String extractFromPdf(String input) {
        if (input != null && input.endsWith(".pdf")) {
            Matcher m = p.matcher(input);
            if (m.find())
                return m.group();
        }
        return "";
    }

    public String extract(String input) {
        if (input != null) {
            Matcher m = p.matcher(input);
            if (m.find())
                return m.group();
        }
        return "";
    }
}
