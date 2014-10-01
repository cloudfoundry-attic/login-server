package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ThymeleafConfig.class)
public class EmailAccountCreationServiceTests {

    private EmailAccountCreationService emailAccountCreationService;
    private MockRestServiceServer mockUaaServer;
    private MessageService messageService;
    private RestTemplate uaaTemplate;

    @Autowired
    @Qualifier("mailTemplateEngine")
    SpringTemplateEngine templateEngine;

    @Before
    public void setUp() throws Exception {
        uaaTemplate = new RestTemplate();
        mockUaaServer = MockRestServiceServer.createServer(uaaTemplate);
        messageService = mock(MessageService.class);
        emailAccountCreationService = new EmailAccountCreationService(new ObjectMapper(), templateEngine, messageService, uaaTemplate, "http://uaa.example.com/uaa", "pivotal");
    }

    @Test
    public void testBeginActivation() throws Exception {
        setUpForSuccess();

        emailAccountCreationService.beginActivation("user@example.com", "password", "login");

        mockUaaServer.verify();

        ArgumentCaptor<String> emailBodyArgument = ArgumentCaptor.forClass(String.class);
        verify(messageService).sendMessage((String) isNull(),
            eq("user@example.com"),
            eq(MessageType.CREATE_ACCOUNT_CONFIRMATION),
            eq("Activate your Pivotal ID"),
            emailBodyArgument.capture()
        );
        String emailBody = emailBodyArgument.getValue();
        assertThat(emailBody, containsString("a Pivotal ID"));
        assertThat(emailBody, containsString("<a href=\"http://localhost/login/accounts/new?code=the_secret_code&amp;email=user%40example.com\">Activate your account</a>"));
        assertThat(emailBody, not(containsString("Cloud Foundry")));
    }

    @Test
    public void testBeginActivationWithOssBrand() throws Exception {
        emailAccountCreationService = new EmailAccountCreationService(new ObjectMapper(), templateEngine, messageService, uaaTemplate, "http://uaa.example.com/uaa", "oss");

        setUpForSuccess();

        emailAccountCreationService.beginActivation("user@example.com", "password", "login");

        ArgumentCaptor<String> emailBodyArgument = ArgumentCaptor.forClass(String.class);
        verify(messageService).sendMessage((String) isNull(),
            eq("user@example.com"),
            eq(MessageType.CREATE_ACCOUNT_CONFIRMATION),
            eq("Activate your account"),
            emailBodyArgument.capture()
        );
        String emailBody = emailBodyArgument.getValue();
        assertThat(emailBody, containsString("an account"));
        assertThat(emailBody, containsString("<a href=\"http://localhost/login/accounts/new?code=the_secret_code&amp;email=user%40example.com\">Activate your account</a>"));
        assertThat(emailBody, not(containsString("Pivotal")));
    }

    @Test
    public void testCompleteActivation() throws Exception {
        Timestamp ts = new Timestamp(System.currentTimeMillis() + (60 * 60 * 1000)); // 1 hour
        String uaaResponseJson = "{" +
            "    \"code\":\"the_secret_code\"," +
            "    \"expiresAt\":" + ts.getTime() + "," +
            "    \"data\":\"{\\\"username\\\":\\\"user@example.com\\\",\\\"client_id\\\":\\\"app\\\"}\"" +
            "}";

        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Codes/the_secret_code"))
            .andExpect(method(GET))
            .andRespond(withSuccess(uaaResponseJson, APPLICATION_JSON));

        String scimUserJSONString = "{" +
            "\"userName\": \"user@example.com\"," +
            "\"id\": \"newly-created-user-id\"," +
            "\"emails\": [{\"value\":\"user@example.com\"}]" +
            "}";


        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Users"))
            .andExpect(method(POST))
            .andExpect(jsonPath("$.userName").value("user@example.com"))
            .andExpect(jsonPath("$.password").value("secret"))
            .andExpect(jsonPath("$.origin").value("uaa"))
            .andExpect(jsonPath("$.emails[0].value").value("user@example.com"))
            .andRespond(withSuccess(scimUserJSONString, APPLICATION_JSON));

        Map<String,Object> additionalInformation = new HashMap<>();
        additionalInformation.put("signup_redirect_url", "http://example.com/redirect");

        String clientDetails = "{" +
                "\"client_id\": \"app\"," +
                "\"signup_redirect_url\": \"http://example.com/redirect\"" +
            "}";

        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/oauth/clients/app"))
            .andExpect(method(GET))
            .andRespond(withSuccess(clientDetails, APPLICATION_JSON));

        AccountCreationService.AccountCreation accountCreation = emailAccountCreationService.completeActivation("the_secret_code", "secret");

        mockUaaServer.verify();

        assertEquals("user@example.com", accountCreation.getUsername());
        assertEquals("newly-created-user-id", accountCreation.getUserId());
        assertEquals("http://example.com/redirect", accountCreation.getRedirectLocation());
        assertNotNull(accountCreation.getUserId());
    }

    @Test
    public void testCompleteActivationWithExpiredCode() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Codes/expiring_code"))
            .andExpect(method(GET))
            .andRespond(withStatus(BAD_REQUEST));

        try {
            emailAccountCreationService.completeActivation("expiring_code", "secret");
            fail();
        } catch(HttpClientErrorException e) {
            assertThat(e.getStatusCode(), Matchers.equalTo(BAD_REQUEST));
        }
    }


    @Test
    public void testCompleteActivationWithExistingUser() throws Exception {
        Timestamp ts = new Timestamp(System.currentTimeMillis() + (60 * 60 * 1000)); // 1 hour
        String uaaResponseJson = "{" +
            "    \"code\":\"the_secret_code\"," +
            "    \"expiresAt\":" + ts.getTime() + "," +
            "    \"data\":\"{\\\"username\\\":\\\"user@example.com\\\",\\\"client_id\\\":\\\"login\\\"}\"" +
            "}";

        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Codes/expiring_code"))
            .andExpect(method(GET))
            .andRespond(withSuccess(uaaResponseJson, APPLICATION_JSON));

        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Users"))
            .andExpect(method(POST))
            .andExpect(jsonPath("$.userName").value("user@example.com"))
            .andExpect(jsonPath("$.password").value("secret"))
            .andExpect(jsonPath("$.origin").value("uaa"))
            .andExpect(jsonPath("$.emails[0].value").value("user@example.com"))
            .andRespond(withStatus(CONFLICT));

        try {
            emailAccountCreationService.completeActivation("expiring_code", "secret");
            fail();
        } catch (HttpClientErrorException e) {
            assertThat(e.getStatusCode(), Matchers.equalTo(CONFLICT));
        }
    }

    private void setUpForSuccess() {
        Timestamp ts = new Timestamp(System.currentTimeMillis() + (60 * 60 * 1000)); // 1 hour

        String uaaResponseJson = "{" +
            "    \"code\":\"the_secret_code\"," +
            "    \"expiresAt\":" + ts.getTime() + "," +
            "    \"data\":\"{\\\"username\\\":\\\"user@example.com\\\",\\\"client_id\\\":\\\"login\\\"}\"" +
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