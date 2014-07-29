package org.cloudfoundry.identity.uaa.login;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.util.UriComponentsBuilder;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;

public class EmailService {

    private final JavaMailSender mailSender;
    private final String loginUrl;
    private final String brand;

    public EmailService(JavaMailSender mailSender, String loginUrl, String brand) {
        this.mailSender = mailSender;
        this.loginUrl = loginUrl;
        this.brand = brand;
    }

    public void sendMimeMessage(String email, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        message.addFrom(getSenderAddresses());
        message.addRecipients(Message.RecipientType.TO, email);
        message.setSubject(subject);
        message.setContent(htmlContent, "text/html");
        mailSender.send(message);
    }

    private Address[] getSenderAddresses() throws AddressException, UnsupportedEncodingException {
        String host = UriComponentsBuilder.fromHttpUrl(loginUrl).build().getHost();
        String name = brand.equals("pivotal") ? "Pivotal" : "Cloud Foundry";
        return new Address[]{new InternetAddress("admin@" + host, name)};
    }

}
