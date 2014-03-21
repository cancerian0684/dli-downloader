package org.shunya.dli;

import org.junit.Test;

public class InternetConnectionTesterTest {
    private InternetConnectionTester tester = new InternetConnectionTester(10, "http://www.google.com");

    @Test
    public void testAwaitAvailability() throws Exception {

    }

    @Test
    public void testIsConnected() throws Exception {
        tester.isConnected();
    }
}
