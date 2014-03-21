package org.shunya.dli;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class DownloadJobs {
    private List<String> barCodes = new ArrayList<>();

    public List<String> getBarCodes() {
        return barCodes;
    }

    @XmlElement
    public void setBarCodes(List<String> barCodes) {
        this.barCodes = barCodes;
    }
}
