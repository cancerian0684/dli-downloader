package org.shunya.dli;

import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.shunya.dli.BarcodeExtractor.instance;

public class BarcodeExtractorTest {
    @Test
    public void testExtractFromFileName() throws Exception {
         assertThat(instance.extractFromPdf("test this file name with barcode :1990010086151, is it right ?.pdf"), equalTo("1990010086151"));
         assertThat(instance.extractFromPdf(" 1990010086151-this is the pdf, is it right ?.pdf"), equalTo("1990010086151"));
    }

    @Test
    public void testExtractFromBlankBarcode() throws Exception {
        assertThat(instance.extractFromPdf("test this file name with barcode :, is it right ?.pdf"), equalTo(""));
    }

    @Test
    public void testExtractForNullInput() throws Exception {
        assertThat(instance.extractFromPdf(null), equalTo(""));
    }
}
