/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.login;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import org.cloudfoundry.identity.uaa.authentication.login.Prompt;
import org.cloudfoundry.identity.uaa.login.saml.IdentityProviderDefinition;
import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SamlRemoteUaaControllerMockMvcTests.ContextConfiguration.class)
public class SamlRemoteUaaControllerMockMvcTests {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    MockEnvironment environment;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testSamlLoginFiltersOutPasscodePromptFromUi() throws Exception {

        mockMvc.perform(get("/login").accept(TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("prompts", hasKey("how")))
                .andExpect(model().attribute("prompts", not(hasKey("passcode"))));

        mockMvc.perform(get("/login").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("prompts", hasKey("how")))
                .andExpect(model().attribute("prompts", hasKey("passcode")));
    }

//    @Test
//    public void testSamlLoginShowsSamlLoginMessage() throws Exception {
//
//        mockMvc.perform(get("/login").accept(TEXT_HTML))
//            .andExpect(status().isOk())
//            .andExpect(view().name("login"))
//            .andExpect(model().attribute("showSamlLoginLink", true))
//            .andExpect(xpath("//a[text()='Use your corporate credentials']").exists());
//
//        environment.setProperty("login.showSamlLoginLink", "false");
//        mockMvc.perform(get("/login").accept(TEXT_HTML))
//            .andExpect(model().attribute("showSamlLoginLink", false))
//            .andExpect(xpath("//a[text()='Use your corporate credentials']").doesNotExist());
//    }

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
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        MockEnvironment environment() {
            return new MockEnvironment();
        }

        @Bean
        SamlRemoteUaaController samlRemoteUaaController(MockEnvironment environment, RestTemplate restTemplate) {
            SamlRemoteUaaController remoteUaaController = new SamlRemoteUaaController(environment, restTemplate);
            Prompt first = new Prompt("how", "text", "How did I get here?");
            Prompt second = new Prompt("passcode", "password", "This should be filtered out of the UI but not the API.");
            remoteUaaController.setPrompts(Arrays.asList(first, second));
            remoteUaaController.setAuthorizationTemplate(restTemplate);
            remoteUaaController.setIdpDefinitions(new ArrayList<IdentityProviderDefinition>());
            return remoteUaaController;
        }
    }
}
