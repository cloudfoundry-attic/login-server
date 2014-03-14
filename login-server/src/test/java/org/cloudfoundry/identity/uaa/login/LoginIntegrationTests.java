package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.config.YamlServletProfileInitializer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.XmlWebApplicationContext;

import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class LoginIntegrationTests {
    private XmlWebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        webApplicationContext = new XmlWebApplicationContext();
        MockServletContext servletContext = new MockServletContext();
        MockServletConfig servletConfig = new MockServletConfig(servletContext);
        servletConfig.addInitParameter("environmentConfigDefaults", "login.yml");
        webApplicationContext.setServletContext(servletContext);
        webApplicationContext.setServletConfig(servletConfig);
        webApplicationContext.setConfigLocation("file:./src/main/webapp/WEB-INF/spring-servlet.xml");
        new YamlServletProfileInitializer().initialize(webApplicationContext);
        webApplicationContext.refresh();

        FilterChainProxy[] filters = webApplicationContext.getBeansOfType(FilterChainProxy.class).values().toArray(new FilterChainProxy[0]);
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilters(filters).build();
    }

    @After
    public void tearDown() throws Exception {
        webApplicationContext.close();
    }

    @Test
    public void testLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("links", hasEntry("home", "https://console.10.244.0.34.xip.io")))
                .andExpect(model().attribute("links", hasEntry("passwd", "https://console.10.244.0.34.xip.io/password_resets/new")))
                .andExpect(model().attribute("links", hasEntry("register", "https://console.10.244.0.34.xip.io/register")))
                .andExpect(model().attribute("links", hasEntry("uaa", "http://localhost:8080/uaa")))
                .andExpect(model().attribute("links", hasEntry("login", "http://localhost:8080/login")))
                .andExpect(model().attributeExists("prompts"))
                .andExpect(model().attributeExists("app"))
                .andExpect(model().attributeExists("commit_id"))
                .andExpect(model().attributeExists("timestamp"))
                .andExpect(model().attributeDoesNotExist("saml"))
                .andExpect(model().attribute("analytics", hasEntry("code", "secret_code")))
                .andExpect(model().attribute("analytics", hasEntry("domain", "example.com")));
    }
}
