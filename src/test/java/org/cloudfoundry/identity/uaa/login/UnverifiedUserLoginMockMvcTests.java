package org.cloudfoundry.identity.uaa.login;

import com.dumbster.smtp.SimpleSmtpServer;
import org.cloudfoundry.identity.uaa.login.test.UaaRestTemplateBeanFactoryPostProcessor;
import org.cloudfoundry.identity.uaa.test.YamlServletProfileInitializerContextInitializer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.support.XmlWebApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UnverifiedUserLoginMockMvcTests {

    XmlWebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private MockRestServiceServer remoteAuthUaaServer;
    private MockRestServiceServer uaaServer;
    private static SimpleSmtpServer mailServer;

    @BeforeClass
    public static void startMailServer() throws Exception {
        mailServer = SimpleSmtpServer.start(2525);
    }

    @Before
    public void setUp() throws Exception {
        webApplicationContext = new XmlWebApplicationContext();
        webApplicationContext.setEnvironment(new MockEnvironment());
        new YamlServletProfileInitializerContextInitializer().initializeContext(webApplicationContext, "login.yml");
        webApplicationContext.setConfigLocation("file:./src/main/webapp/WEB-INF/spring-servlet.xml");
        webApplicationContext.addBeanFactoryPostProcessor(new UaaRestTemplateBeanFactoryPostProcessor("remoteAuthManagerAuthorizationTemplate"));
        webApplicationContext.addBeanFactoryPostProcessor(new UaaRestTemplateBeanFactoryPostProcessor("authorizationTemplate"));
        webApplicationContext.refresh();
        FilterChainProxy springSecurityFilterChain = webApplicationContext.getBean("springSecurityFilterChain", FilterChainProxy.class);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(springSecurityFilterChain)
                .build();

        remoteAuthUaaServer = MockRestServiceServer.createServer(webApplicationContext.getBean("remoteAuthManagerAuthorizationTemplate", RestTemplate.class));
        uaaServer = MockRestServiceServer.createServer(webApplicationContext.getBean("authorizationTemplate", RestTemplate.class));
    }

    @AfterClass
    public static void stopMailServer() throws Exception {
        mailServer.stop();
    }

    @Test
    public void testUnverifiedUserLoginUnsuccessfully() throws Exception {
        remoteAuthUaaServer.expect(requestTo("http://localhost:8080/uaa/authenticate"))
            .andExpect(method(POST))
            .andRespond(new AuthenticateForbiddenResponseCreator());

        String uaaResponse = "{\n" +
                "  \"resources\": [\n" +
                "    {\n" +
                "      \"id\": \"9cb20c4f-2cc3-449e-9619-5cd61a6185d9\",\n" +
                "      \"userName\": \"unverified@example.com\",\n" +
                "      \"origin\": \"uaa\"\n" +
                "    }\n" +
                "  ]}";


        uaaServer.expect(requestTo("http://localhost:8080/uaa/ids/Users?attributes=id&filter=userName%20eq%20%22unverified@example.com%22%20and%20origin%20eq%20%22uaa%22"))
        .andRespond(withSuccess(uaaResponse, APPLICATION_JSON));

        uaaServer.expect(requestTo("http://localhost:8080/uaa/Codes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.data").value(containsString("\"client_id\":\"login\"")))
                .andRespond(withSuccess("{\"code\":\"the_secret_code\"," +
                                "\"expiresAt\":1406152741265," +
                                "\"data\":\"{\\\"user_id\\\":\\\"newly-created-user-id\\\",\\\"client_id\\\":\\\"login\\\"}\"}",
                        APPLICATION_JSON));


        mockMvc.perform(post("/login.do")
                .param("username", "unverified@example.com")
                .param("password", "secret")
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login?error=account_not_verified"));
    }


    @Test
    public void testUnverifiedUserLoginIsForbiddenViaPasswordGrant() throws Exception {
        remoteAuthUaaServer.expect(requestTo("http://localhost:8080/uaa/authenticate"))
            .andExpect(method(POST))
            .andRespond(new AuthenticateForbiddenResponseCreator());

        mockMvc.perform(post("/oauth/token")
                .param("username", "unverified@example.com")
                .param("password", "secret")
                .param("grant_type", "password")
                .contentType(APPLICATION_JSON)
                .accept(APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    private static class AuthenticateForbiddenResponseCreator implements ResponseCreator {
        @Override
        public ClientHttpResponse createResponse(ClientHttpRequest request) throws IOException {
            return new ClientHttpResponse() {
                @Override
                public HttpStatus getStatusCode() throws IOException {
                    return HttpStatus.FORBIDDEN;
                }

                @Override
                public int getRawStatusCode() throws IOException {
                    return 403;
                }

                @Override
                public String getStatusText() throws IOException {
                    return "Forbidden";
                }

                @Override
                public void close() {}

                @Override
                public InputStream getBody() throws IOException {
                    return new ByteArrayInputStream("{\"error\": \"account not verified\"}".getBytes());
                }

                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Content-Type", "application/json");
                    return headers;
                }
            };
        }
    }
}
