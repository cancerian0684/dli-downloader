package org.shunya.dli;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;

public class UtilsTest {
    @Test
    @Ignore
    public void testReadFromFile() throws Exception {
        String contents = Utils.readFromFile(Paths.get(UtilsTest.class.getClassLoader().getResource("barcodes.txt").toURI()));
        String[] strings = Utils.extractBarcodes(contents);
        for (String string : strings) {
            System.out.println("contents = " + string);
        }
    }

    @Test
    public void testCleanText() {
        String clean = Utils.clean("my name is Munish   none is my None NULL Null  order null values i \"have");
        System.out.println(clean);
    }
}
