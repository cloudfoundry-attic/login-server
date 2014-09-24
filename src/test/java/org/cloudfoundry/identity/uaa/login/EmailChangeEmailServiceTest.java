package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.sql.Timestamp;

import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ThymeleafConfig.class)
public class EmailChangeEmailServiceTest {
    private EmailChangeEmailService emailChangeEmailService;
    private MockRestServiceServer mockUaaServer;
    private MockEnvironment mockEnvironment;
    private MessageService messageService;

    @Autowired
    @Qualifier("mailTemplateEngine")
    SpringTemplateEngine templateEngine;

    @Before
    public void setUp() throws Exception {
        RestTemplate uaaTemplate = new RestTemplate();
        mockUaaServer = MockRestServiceServer.createServer(uaaTemplate);
        messageService = Mockito.mock(EmailService.class);
        mockEnvironment = new MockEnvironment();
        emailChangeEmailService = new EmailChangeEmailService(templateEngine, messageService, uaaTemplate, "http://uaa.example.com/uaa", "pivotal", new ObjectMapper());
    }


    @Test
    @Ignore("Waiting for email text")
    public void beginEmailChange() throws Exception {
        setUpForSuccess();

        emailChangeEmailService.beginEmailChange("user-001", "user@example.com", "new@example.com");

        mockUaaServer.verify();

        Mockito.verify(messageService).sendMessage(
            eq("new@example.com"),
            eq(MessageType.CHANGE_EMAIL),
            eq("Email change verification"),
            contains("<a href=\"http://localhost/login/verify_email?code=the_secret_code\">Verify your Email</a>")
        );
    }

    @Test
    public void testCompleteVerification() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/email_changes"))
            .andExpect(method(POST))
            .andExpect(content().string("the_secret_code"))
            .andRespond(withSuccess("{" +
                "  \"user_id\":\"user-001\"," +
                "  \"username\":\"new@example.com\"," +
                "  \"email\": \"new@example.com\" " +
                "}", APPLICATION_JSON));

        emailChangeEmailService.completeVerification("the_secret_code");

        mockUaaServer.verify();
    }

    private void setUpForSuccess() {
        Timestamp ts = new Timestamp(System.currentTimeMillis() + (60 * 60 * 1000)); // 1 hour

        String uaaResponseJson = "{" +
            "    \"code\":\"the_secret_code\"," +
            "    \"expiresAt\":" + ts.getTime() + "," +
            "    \"data\":\"{\\\"userId\\\":\\\"user-001\\\",\\\"userId\\\":\\\"new@example.com\\\"}\"" +
            "}";

        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Codes"))
            .andExpect(method(POST))
            .andExpect(jsonPath("$.expiresAt").value(Matchers.greaterThan(ts.getTime() - 5000)))
            .andExpect(jsonPath("$.expiresAt").value(Matchers.lessThan(ts.getTime() + 5000)))
            .andExpect(jsonPath("$.data").exists()) // we can't tell what order the json keys will take in the serialized json, so exists is the best we can do
            .andRespond(withSuccess(uaaResponseJson, APPLICATION_JSON));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("http");
        request.setContextPath("/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}