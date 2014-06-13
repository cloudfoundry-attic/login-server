package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AccountsControllerViewTests.ContextConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AccountsControllerViewTests {

    @Autowired
    WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
    }

    @Test
    public void testTiles() throws Exception {
        mockMvc.perform(get("/accounts/new"))
                .andExpect(xpath("//*[@class='tile-1']").string("First Tile"))
                .andExpect(xpath("//*[@class='tile-1']/@href").string("http://example.com/signup"))
                .andExpect(xpath("//head/style[1]").string(".tile-1 {background-image: url(//example.com/image)} .tile-1:hover {background-image: url(//example.com/hover)}"))
                .andExpect(xpath("//*[@class='tile-2']").string("Other Tile"))
                .andExpect(xpath("//*[@class='tile-2']/@href").string("http://other.example.com/signup"))
                .andExpect(xpath("//head/style[2]").string(".tile-2 {background-image: url(//other.example.com/image)} .tile-2:hover {background-image: url(//other.example.com/hover)}"));
    }

    @Configuration
    @EnableWebMvc
    @Import(ThymeleafConfig.class)
    static class ContextConfiguration extends WebMvcConfigurerAdapter {

        @Override
        public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
            configurer.enable();
        }

        @Bean
        BuildInfo buildInfo() {
            return new BuildInfo();
        }

        @Bean
        TileInfo tileInfo() {
            Map<String,String> tile1 = new LinkedHashMap<>();
            tile1.put("name", "First Tile");
            tile1.put("signup-link", "http://example.com/signup");
            tile1.put("image", "//example.com/image");
            tile1.put("image-hover", "//example.com/hover");

            Map<String,String> tile2 = new LinkedHashMap<>();
            tile2.put("name", "Other Tile");
            tile2.put("signup-link", "http://other.example.com/signup");
            tile2.put("image", "//other.example.com/image");
            tile2.put("image-hover", "//other.example.com/hover");

            TileInfo tileInfo = Mockito.mock(TileInfo.class);
            Mockito.when(tileInfo.getSignupTiles()).thenReturn(Arrays.asList(tile1, tile2));
            return tileInfo;
        }

        @Bean
        AccountsController accountsController() {
            return new AccountsController();
        }
    }
}