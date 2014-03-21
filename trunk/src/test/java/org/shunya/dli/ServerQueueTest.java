package org.shunya.dli;

public class ServerQueueTest {
    private final String[] rootUrl = {
            "first",
            "second",
            "third",
            "fourth",
            "fifth",
            "sixth",
    };

//    private ServerQueue serverQueue = new ServerQueue(rootUrl);

  /*  @Test
    public void testGetNextServer() throws Exception {
        String nextServer1 = serverQueue.getNextServer();
        serverQueue.markNotWorking();
        String nextServer2 = serverQueue.getNextServer();
        assertThat(nextServer1, equalTo(rootUrl[0]));
        assertThat(nextServer2, equalTo(rootUrl[1]));
        System.out.println("serverQueue = " + serverQueue);
    }

    @Test
    public void testNextServerMultipleTimes() {
        String nextServer1 = serverQueue.getNextServer();
        String nextServer2 = serverQueue.getNextServer();
        assertThat(nextServer1, equalTo(rootUrl[0]));
        assertThat(nextServer2, equalTo(rootUrl[0]));
    }*/
}
