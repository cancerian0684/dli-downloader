package org.shunya.dli;

import java.util.*;

public class ServerQueue {
    /*final String[] rootUrl = {
            "http://www.dli.gov.in",
            "http://www.new.dli.gov.in",
            "http://202.41.82.144",
            "http://www.new.dli.ernet.in",
            "http://www.new1.dli.ernet.in",
            "http://www.dli.ernet.in",
    };*/

    private final Comparator<DLIServer> serverMetadataComparator = (o1, o2) -> Integer.compare(o1.getMetadataCount().get(),o2.getMetadataCount().get() );

    private final Comparator<DLIServer> serverDownloadComparator = (o1, o2) -> Integer.compare(o1.getDownloadCount().get(), o2.getDownloadCount().get());

    private Set<DLIServer> serverList = new HashSet<>(10);

    public ServerQueue(final String urls) {
        final String[] urlArray = urls.split("[;,]");
        for (String url : urlArray) {
            if (!url.isEmpty()) {
                serverList.add(new DLIServer(url));
            }
        }
    }

    public List<DLIServer> getDownloadServers() {
        List<DLIServer> copy = new ArrayList<>(serverList);
        Collections.sort(copy, serverDownloadComparator);
        return copy;
    }

    public List<DLIServer> getMetadataServers() {
        List<DLIServer> copy = new ArrayList<>(serverList);
        Collections.sort(copy, serverMetadataComparator);
        return copy;
    }

    @Override
    public synchronized String toString() {
        return serverList.toString();
    }
}
