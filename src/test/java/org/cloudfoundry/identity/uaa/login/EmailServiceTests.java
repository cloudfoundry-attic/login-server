package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.login.test.FakeJavaMailSender;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.POST;

public class EmailServiceTests {

    private FakeJavaMailSender mailSender;

    @Before
    public void setUp() throws Exception {
        mailSender = new FakeJavaMailSender();
    }

    @Test
    public void testSendOssMimeMessage() throws Exception {
        EmailService emailService = new EmailService(mailSender, "http://login.example.com/login", "oss", null, null);

        emailService.sendMimeMessage("user@example.com", "Test Message", "<html><body>hi</body></html>");

        assertThat(mailSender.getSentMessages(), hasSize(1));
        FakeJavaMailSender.MimeMessageWrapper mimeMessageWrapper = mailSender.getSentMessages().get(0);
        assertThat(mimeMessageWrapper.getFrom(), hasSize(1));
        InternetAddress fromAddress = (InternetAddress) mimeMessageWrapper.getFrom().get(0);
        assertThat(fromAddress.getAddress(), equalTo("admin@login.example.com"));
        assertThat(fromAddress.getPersonal(), equalTo("Cloud Foundry"));
        assertThat(mimeMessageWrapper.getRecipients(Message.RecipientType.TO), hasSize(1));
        assertThat(mimeMessageWrapper.getRecipients(Message.RecipientType.TO).get(0), equalTo((Address) new InternetAddress("user@example.com")));
        assertThat(mimeMessageWrapper.getContentString(), equalTo("<html><body>hi</body></html>"));
    }

    @Test
    public void testSendPivotalMimeMessage() throws Exception {
        EmailService emailService = new EmailService(mailSender, "http://login.example.com/login", "pivotal", null, null);

        emailService.sendMimeMessage("user@example.com", "Test Message", "<html><body>hi</body></html>");

        FakeJavaMailSender.MimeMessageWrapper mimeMessageWrapper = mailSender.getSentMessages().get(0);
        assertThat(mimeMessageWrapper.getFrom(), hasSize(1));
        InternetAddress fromAddress = (InternetAddress) mimeMessageWrapper.getFrom().get(0);
        assertThat(fromAddress.getAddress(), equalTo("admin@login.example.com"));
        assertThat(fromAddress.getPersonal(), equalTo("Pivotal"));
    }

    @Test
    public void testSendNotification() throws Exception {
        RestTemplate notificationsTemplate = Mockito.mock(RestTemplate.class);
        EmailService emailService = new EmailService(null, null, null, notificationsTemplate, "http://example.com");

        emailService.sendNotification("user-id-01", "kind-id-01", "Subject", "<p>Text</p>");

        ArgumentCaptor<HttpEntity<Map<String,String>>> requestArgument = ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(notificationsTemplate).exchange(eq("http://example.com/users/user-id-01"), eq(POST), requestArgument.capture(), eq(Void.class));
        HttpEntity<Map<String, String>> httpRequest = requestArgument.getValue();
        Map<String,String> request = httpRequest.getBody();
        assertThat(request.values(), containsInAnyOrder("kind-id-01", "Subject", "<p>Text</p>"));
    }
}