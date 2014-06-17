package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.login.test.FakeJavaMailSender;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class EmailServiceTests {

    private FakeJavaMailSender mailSender;

    @Before
    public void setUp() throws Exception {
        mailSender = new FakeJavaMailSender();
    }

    @Test
    public void testSendOssMimeMessage() throws Exception {
        EmailService emailService = new EmailService(mailSender, "http://login.example.com/login", "oss");

        emailService.sendMimeMessage("user@example.com", "Test Message", "<html><body>hi</body></html>");

        Assert.assertThat(mailSender.getSentMessages(), hasSize(1));
        FakeJavaMailSender.MimeMessageWrapper mimeMessageWrapper = mailSender.getSentMessages().get(0);
        Assert.assertThat(mimeMessageWrapper.getFrom(), hasSize(1));
        Assert.assertThat(mimeMessageWrapper.getFrom().get(0), equalTo((Address) new InternetAddress("admin@login.example.com", "Cloud Foundry")));
        Assert.assertThat(mimeMessageWrapper.getRecipients(Message.RecipientType.TO), hasSize(1));
        Assert.assertThat(mimeMessageWrapper.getRecipients(Message.RecipientType.TO).get(0), equalTo((Address) new InternetAddress("user@example.com")));
        Assert.assertThat(mimeMessageWrapper.getContentString(), equalTo("<html><body>hi</body></html>"));
    }

    @Test
    public void testSendPivotalMimeMessage() throws Exception {
        EmailService emailService = new EmailService(mailSender, "http://login.example.com/login", "pivotal");

        emailService.sendMimeMessage("user@example.com", "Test Message", "<html><body>hi</body></html>");

        FakeJavaMailSender.MimeMessageWrapper mimeMessageWrapper = mailSender.getSentMessages().get(0);
        Assert.assertThat(mimeMessageWrapper.getFrom(), hasSize(1));
        Assert.assertThat(mimeMessageWrapper.getFrom().get(0), equalTo((Address) new InternetAddress("admin@login.example.com", "Pivotal")));
    }
}