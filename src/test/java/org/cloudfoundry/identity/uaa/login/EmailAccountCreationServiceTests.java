package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.sql.Timestamp;

import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ThymeleafConfig.class)
public class EmailAccountCreationServiceTests {

    private EmailAccountCreationService emailAccountCreationService;
    private MockRestServiceServer mockUaaServer;
    private EmailService emailService;

    @Autowired
    @Qualifier("mailTemplateEngine")
    SpringTemplateEngine templateEngine;

    @Before
    public void setUp() throws Exception {
        RestTemplate uaaTemplate = new RestTemplate();
        mockUaaServer = MockRestServiceServer.createServer(uaaTemplate);
        emailService = Mockito.mock(EmailService.class);
        emailAccountCreationService = new EmailAccountCreationService(templateEngine, emailService, uaaTemplate, "http://uaa.example.com/uaa", "pivotal");
    }

    @Test
    public void testBeginActivation() throws Exception {
        Timestamp ts = new Timestamp(System.currentTimeMillis() + (60 * 60 * 1000)); // 1 hour

        String uaaResponseJson = "{" +
                "    \"code\":\"the_secret_code\"," +
                "    \"expiresAt\":" + ts.getTime() + "," +
                "    \"data\":\"user@example.com\"" +
                "}";

        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Codes"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.expiresAt").value(Matchers.greaterThan(ts.getTime() - 5000)))
                .andExpect(jsonPath("$.expiresAt").value(Matchers.lessThan(ts.getTime() + 5000)))
                .andExpect(jsonPath("$.data").value("user@example.com"))
                .andRespond(withSuccess(uaaResponseJson, APPLICATION_JSON));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setProtocol("http");
        request.setContextPath("/login");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        emailAccountCreationService.beginActivation("user@example.com");

        mockUaaServer.verify();

        Mockito.verify(emailService).sendMimeMessage(
                eq("user@example.com"),
                eq("Pivotal account activation request"),
                contains("<a href=\"http://localhost/login/accounts/new?code=the_secret_code&amp;email=user%40example.com\">Activate your account</a>")
        );
    }

    @Test
    public void testCompleteActivation() throws Exception {
        String responseJson = "{\"user_id\":\"newly-created-user-id\",\"username\":\"user@example.com\"}";
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/create_account"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.code").value("expiring_code"))
                .andExpect(jsonPath("$.password").value("secret"))
                .andRespond(withSuccess(responseJson, APPLICATION_JSON));    // *
        // * uaa actually returns a created status, but MockRestServiceServer doesn't support the created code with a response body

        AccountCreationService.Account account = emailAccountCreationService.completeActivation("expiring_code", "secret");

        mockUaaServer.verify();

        Assert.assertEquals("user@example.com", account.getUsername());
        Assert.assertEquals("newly-created-user-id", account.getUserId());
    }

    @Test
    public void testCompleteActivationWithExpiredCode() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/create_account"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.code").value("expiring_code"))
                .andExpect(jsonPath("$.password").value("secret"))
                .andRespond(withBadRequest());

        try {
            emailAccountCreationService.completeActivation("expiring_code", "secret");
            Assert.fail();
        } catch (HttpClientErrorException e) {
            Assert.assertThat(e.getStatusCode(), Matchers.equalTo(BAD_REQUEST));
        }
    }

    @Test
    public void testCompleteActivationWithExistingUser() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/create_account"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.code").value("expiring_code"))
                .andExpect(jsonPath("$.password").value("secret"))
                .andRespond(withStatus(CONFLICT));

        try {
            emailAccountCreationService.completeActivation("expiring_code", "secret");
            Assert.fail();
        } catch (HttpClientErrorException e) {
            Assert.assertThat(e.getStatusCode(), Matchers.equalTo(CONFLICT));
        }
    }
}