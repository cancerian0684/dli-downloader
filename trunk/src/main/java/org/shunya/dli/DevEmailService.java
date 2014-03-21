package org.shunya.dli;

import com.sun.mail.smtp.SMTPMessage;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class DevEmailService {
    private static DevEmailService emailService;
    private final Properties properties;

    public static DevEmailService getInstance() {
        if (emailService == null) {
            emailService = new DevEmailService();
        }
        return emailService;
    }

    public static void main(String[] args) {
        DevEmailService.getInstance().sendEmail("Hi-Munish Test Email", "cancerian0684@gmail.com", "<h3>Hello world</h3> this is a test email", Collections.<File>emptyList());
    }

    private DevEmailService() {
        properties = new Properties();
        try {
            properties.load(DevEmailService.class.getResourceAsStream("/devMail.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendEmail(String subject, String commaSeparatedRecipients, String body, List<File> attachments) {
        Session session = Session.getDefaultInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(properties.getProperty("mail.smtp.user"), properties.getProperty("mail.smtp.password"));
                    }
                });
        try {
            SMTPMessage message = new SMTPMessage(session);
            message.setFrom(new InternetAddress(properties.getProperty("mail.sender")));
            if (commaSeparatedRecipients == null || commaSeparatedRecipients.isEmpty())
                commaSeparatedRecipients = properties.getProperty("author.email");
            message.addRecipients(Message.RecipientType.TO, getAddresses(commaSeparatedRecipients));
            message.setSubject(subject);
//            message.setHeader("Content-Type", "text/html; charset=UTF-8");
//            message.setText( body, "UTF-8", "html" );
//            message.setContent(body, "text/html");
            message.saveChanges();

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setHeader("Content-Type", "text/html; charset=UTF-8");
//            messageBodyPart.setContent(body, "text/html");
            messageBodyPart.setText(body, "UTF-8", "html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            // Part two is attachment
            for (File file : attachments) {
                if (file == null || !file.exists())
                    continue;
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setDataHandler(new DataHandler(new FileDataSource(file)));
                messageBodyPart.setFileName(file.getName());
                multipart.addBodyPart(messageBodyPart);
            }
            // Put parts in message
            message.setContent(multipart);
            message.setSendPartial(true);
//            Transport transport = session.getTransport("smtps");
//            transport.connect(properties.getProperty("mail.smtp.host"), properties.getProperty("mail.smtp.user"), properties.getProperty("mail.smtp.password"));
//            transport.sendMessage(message, message.getAllRecipients());
//            transport.close();
            Transport.send(message);
            System.out.println("Sent Expense successfully....");
        } catch (MessagingException mex) {
            throw new RuntimeException(mex);
        }
    }

    public void sendEmail(String subject, String commaSeparatedRecipients, String cc, String body, List<byte[]> attachments, List<String> fileNames) {
        Session session = Session.getDefaultInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(properties.getProperty("mail.smtp.user"), properties.getProperty("mail.smtp.password"));
                    }
                });
        try {
            SMTPMessage message = new SMTPMessage(session);
            message.setFrom(new InternetAddress(properties.getProperty("mail.sender")));
            if (commaSeparatedRecipients == null || commaSeparatedRecipients.isEmpty())
                commaSeparatedRecipients = properties.getProperty("author.email");
            if (commaSeparatedRecipients != null && !commaSeparatedRecipients.trim().isEmpty())
                message.addRecipients(Message.RecipientType.TO, getAddresses(commaSeparatedRecipients));
            if (cc != null && !cc.trim().isEmpty())
                message.addRecipients(Message.RecipientType.CC, getAddresses(cc));
            message.setSubject(subject);
            message.saveChanges();

            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setHeader("Content-Type", "text/html; charset=UTF-8");
//            messageBodyPart.setContent(body, "text/html");
            messageBodyPart.setText(body, "UTF-8", "html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            // Part two is attachment
            int i = 0;
            for (byte[] bytes : attachments) {
                messageBodyPart = new MimeBodyPart();
                messageBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(bytes, "text/plain")));
                messageBodyPart.setFileName(fileNames.get(i));
                multipart.addBodyPart(messageBodyPart);
                i++;
            }
            // Put parts in message
            message.setContent(multipart);
            message.setSendPartial(true);
//            Transport transport = session.getTransport("smtps");
//            transport.connect(properties.getProperty("mail.smtp.host"), properties.getProperty("mail.smtp.user"), properties.getProperty("mail.smtp.password"));
//            transport.sendMessage(message, message.getAllRecipients());
//            transport.close();
            Transport.send(message);
            System.out.println("Sent Expense successfully....");
        } catch (MessagingException mex) {
            throw new RuntimeException("Problem sending email", mex);
        }
    }

    public InternetAddress[] getAddresses(String commaSeparatedRecipients) throws AddressException {
        String[] recipients = commaSeparatedRecipients.split("[,;]");
        InternetAddress[] addresses = new InternetAddress[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            addresses[i] = new InternetAddress(recipients[i]);
        }
        return addresses;
    }
}