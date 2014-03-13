package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.config.YamlServletProfileInitializer;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.XmlWebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class LoginIntegrationTests {
    private XmlWebApplicationContext webApplicationContext;
    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        webApplicationContext = new XmlWebApplicationContext();
        webApplicationContext.setServletContext(new MockServletContext());
        webApplicationContext.setConfigLocation("file:./src/main/webapp/WEB-INF/spring-servlet.xml");
        new YamlServletProfileInitializer().initialize(webApplicationContext);
        webApplicationContext.refresh();
        FilterChainProxy springSecurityFilterChain = webApplicationContext.getBean(FilterChainProxy.class);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilter(springSecurityFilterChain).build();
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
                .andExpect(model().attribute("links", Matchers.hasEntry("home", "https://www.cloudfoundry.com")))
                .andExpect(model().attribute("links", Matchers.hasEntry("passwd", "https://www.cloudfoundry.com/passwd")))
                .andExpect(model().attribute("links", Matchers.hasEntry("register", "https://www.cloudfoundry.com/signup")))
                .andExpect(model().attribute("links", Matchers.hasEntry("uaa", "http://localhost:8080/uaa")))
                .andExpect(model().attribute("links", Matchers.hasEntry("login", "http://localhost:8080/login")))
                .andExpect(model().attributeExists("prompts"))
                .andExpect(model().attributeExists("app"))
                .andExpect(model().attributeExists("commit_id"))
                .andExpect(model().attributeExists("timestamp"))
                .andExpect(model().attributeDoesNotExist("saml"))
                .andExpect(model().attributeDoesNotExist("analytics"));
    }
}
