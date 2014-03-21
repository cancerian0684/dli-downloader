package org.shunya.dli;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class AppContextTest {

    @Test
    public void testRegexCleanup() {
        AppContext appContext = new AppContext();
        String badInput = "barcode : 1005689564654 my name is \"munish\", what is * < > \\ / yours : |?.pdf";
        final String replaceAll = badInput.replaceAll(appContext.getCleanupRegex(), appContext.getCleanupChar());
        System.out.println("replaceAll = " + replaceAll);
        assertThat(replaceAll, equalTo("barcode  1005689564654 my name is munish, what is      yours  .pdf"));
    }
}
