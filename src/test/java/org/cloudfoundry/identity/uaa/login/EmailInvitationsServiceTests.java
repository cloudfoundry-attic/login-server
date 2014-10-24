package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.spring4.SpringTemplateEngine;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ThymeleafConfig.class)
public class EmailInvitationsServiceTests {
    private EmailInvitationsService emailInvitationsService;
    private EmailService emailService;

    @Autowired
    @Qualifier("mailTemplateEngine")
    SpringTemplateEngine templateEngine;

    @Before
    public void setUp() throws Exception {
        emailService = Mockito.mock(EmailService.class);
        emailInvitationsService = new EmailInvitationsService(templateEngine, emailService, "pivotal");
    }

    @Test
    public void testSendInviteEmail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("http");
        request.setContextPath("/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        emailInvitationsService.inviteUser("user@example.com", "current-user","login");

        ArgumentCaptor<String> emailBodyArgument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(emailService).sendMimeMessage(
            eq("user@example.com"),
            eq("Invitation to join Pivotal"),
            emailBodyArgument.capture()
        );
        String emailBody = emailBodyArgument.getValue();
        assertThat(emailBody, containsString("current-user"));
        assertThat(emailBody, containsString("Pivotal"));
        assertThat(emailBody, containsString("<a href=\"http://localhost/login/create_account\">Create an Account</a>"));
        assertThat(emailBody, not(containsString("Cloud Foundry")));
    }
}
