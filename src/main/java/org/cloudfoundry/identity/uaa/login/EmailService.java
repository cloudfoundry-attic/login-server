package org.cloudfoundry.identity.uaa.login;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class EmailService {

    private final JavaMailSender mailSender;
    private final String loginUrl;
    private final String brand;
    private final RestTemplate notificationsTemplate;
    private final String notificationsUrl;

    public EmailService(JavaMailSender mailSender, String loginUrl, String brand, RestTemplate notificationsTemplate, String notificationsUrl) {
        this.mailSender = mailSender;
        this.loginUrl = loginUrl;
        this.brand = brand;
        this.notificationsTemplate = notificationsTemplate;
        this.notificationsUrl = notificationsUrl;
    }

    public void sendMimeMessage(String email, String subject, String htmlContent) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        message.addFrom(getSenderAddresses());
        message.addRecipients(Message.RecipientType.TO, email);
        message.setSubject(subject);
        message.setContent(htmlContent, "text/html");
        mailSender.send(message);
    }

    public void sendNotification(String userId, String kindId, String subject, String htmlContent) {
        Map<String,String> request = new HashMap<>();
        request.put("kind_id", kindId);
        request.put("subject", subject);
        request.put("text", htmlContent);
        HttpEntity<Map<String,String>> requestEntity = new HttpEntity<>(request);
        notificationsTemplate.exchange(notificationsUrl + "/users/" + userId, HttpMethod.POST, requestEntity, Void.class);
    }

    private Address[] getSenderAddresses() throws AddressException, UnsupportedEncodingException {
        String host = UriComponentsBuilder.fromHttpUrl(loginUrl).build().getHost();
        String name = brand.equals("pivotal") ? "Pivotal" : "Cloud Foundry";
        return new Address[]{new InternetAddress("admin@" + host, name)};
    }

}
