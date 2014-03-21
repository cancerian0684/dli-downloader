package org.shunya.dli;

import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static java.util.Arrays.asList;

public class DevEmailServiceTest {
    @Test
    @Ignore
    public void testSendEmail() throws Exception {
        DevEmailService.getInstance().sendEmail("Hi-Munish Test Email", "cancerian0684@gmail.com", "<h3>Hello world</h3> this is a test email", Collections.<File>emptyList());
    }

    @Test
    @Ignore
    public void testSendEmail1() throws Exception {
        StringWriter sw = new StringWriter();
        sw.write(Utils.getException(new Exception("man made exception")));
        sw.write("\n\n");
        Utils.listSystemProperties(new PrintWriter(sw));
        DevEmailService.getInstance().sendEmail("Hi-Munish Test Email", "cancerian0684@gmail.com","xxx@gm", "<h3>Hello world</h3> this is a test email", asList(sw.toString().getBytes()), asList("Test attachment.txt"));
    }
}
