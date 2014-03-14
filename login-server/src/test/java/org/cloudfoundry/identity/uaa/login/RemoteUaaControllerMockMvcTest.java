package org.cloudfoundry.identity.uaa.login;

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class RemoteUaaControllerMockMvcTest {

    @Test
    public void testLoginWithoutAnalytics() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        MockMvc mockMvc = getMockMvc(new RemoteUaaController(environment));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attributeDoesNotExist("analytics"));
    }

    @Test
    public void testLoginWithAnalytics() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("analytics.code", "secret_code");
        environment.setProperty("analytics.domain", "example.com");
        MockMvc mockMvc = getMockMvc(new RemoteUaaController(environment));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("analytics", hasEntry("code", "secret_code")))
                .andExpect(model().attribute("analytics", hasEntry("domain", "example.com")));
    }

    private MockMvc getMockMvc(RemoteUaaController controller) {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/jsp/pivotal");
        viewResolver.setSuffix(".jsp");
        return MockMvcBuilders
                .standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }
}
