package docserver;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = Application.class, loader = SpringApplicationContextLoader.class)
public class ApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void apiDocsJson() throws Exception {
        MediaType mediaTypeOfRestService = MediaType.parseMediaType("application/json;charset=UTF-8");

        mockMvc.perform(get("/api-docs")
                .accept(mediaTypeOfRestService))
                .andExpect(status().isOk())
                .andExpect(content().contentType(mediaTypeOfRestService))
                .andExpect(jsonPath("$.swaggerVersion", is("1.2")))
                .andExpect(jsonPath("$.apis", hasSize(2)))
                .andReturn();
    }

    @Test
    public void apiJson() throws Exception {
        MediaType mediaTypeOfRestService = MediaType.parseMediaType("application/json;charset=UTF-8");

        mockMvc.perform(get("/api-docs/AccessController")
                .accept(mediaTypeOfRestService))
                .andExpect(status().isOk())
                .andExpect(content().contentType(mediaTypeOfRestService))
                .andExpect(jsonPath("$.resourcePath", is("/AccessController")))
                .andExpect(jsonPath("$.apis", hasSize(2)))
                .andReturn();
    }
}
