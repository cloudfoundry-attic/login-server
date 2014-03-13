package org.cloudfoundry.identity.uaa.login;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class RemoteUaaControllerMockMvcTest {

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        RemoteUaaController controller = new RemoteUaaController();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(getInternalResourceViewResolver())
                .build();
    }

    @Test
    public void testLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("login"));
    }

    private InternalResourceViewResolver getInternalResourceViewResolver() {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/jsp/pivotal");
        viewResolver.setSuffix(".jsp");
        return viewResolver;
    }
}
