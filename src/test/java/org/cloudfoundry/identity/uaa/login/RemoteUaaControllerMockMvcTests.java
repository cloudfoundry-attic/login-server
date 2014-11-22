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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.cloudfoundry.identity.uaa.authentication.login.Prompt;
import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.util.Arrays;

public class RemoteUaaControllerMockMvcTests {

    @Test
    public void testLoginWithExplicitPrompts() throws Exception {
        RemoteUaaController controller = new RemoteUaaController(new MockEnvironment(), new RestTemplate());
        Prompt first = new Prompt("how", "text", "How did I get here?");
        Prompt second = new Prompt("where", "password", "Where does that highway go to?");
        controller.setPrompts(Arrays.asList(first, second));
        
        MockMvc mockMvc = getMockMvc(controller);

        mockMvc.perform(get("/login").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("prompts", hasKey("how")))
                .andExpect(model().attribute("prompts", hasKey("where")))
                .andExpect(model().attribute("prompts", not(hasKey("password"))));
    }

    @Test
    public void testLoginWithRemoteUaaPrompts() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        RemoteUaaController controller = new RemoteUaaController(new MockEnvironment(), restTemplate);
        controller.setUaaBaseUrl("https://uaa.example.com");

        MockMvc mockMvc = getMockMvc(controller);

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("https://uaa.example.com/login"))
                    .andExpect(method(GET))
                    .andExpect(header("Accept", APPLICATION_JSON_VALUE))
                    .andRespond(withSuccess("{\n" +
                            "    \"prompts\": {\n" +
                            "        \"how\": [\n" +
                            "            \"text\",\n" +
                            "            \"Made-up field.\"\n" +
                            "        ],\n" +
                            "        \"passcode\": [\n" +
                            "            \"password\",\n" +
                            "            \"Passcode should not be filtered out in API.\"\n" +
                            "        ]\n" +
                            "    }\n" +
                            "}", APPLICATION_JSON));

        mockMvc.perform(get("/login").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("prompts", hasKey("how")))
                .andExpect(model().attribute("prompts", hasKey("passcode")))
                .andExpect(model().attribute("prompts", not(hasKey("password"))));
    }

    @Test
    public void testLoginWithDefaultPrompts() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        RemoteUaaController controller = new RemoteUaaController(new MockEnvironment(), restTemplate);
        controller.setUaaBaseUrl("https://uaa.example.com");

        MockMvc mockMvc = getMockMvc(controller);

        MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);
        mockServer.expect(requestTo("https://uaa.example.com/login"))
                .andExpect(method(GET))
                .andExpect(header("Accept", APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("", APPLICATION_JSON));

        mockMvc.perform(get("/login").accept(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("prompts", hasKey("username")))
                .andExpect(model().attribute("prompts", hasKey("password")));
    }

    @Test
    public void testDefaultSignupLink() throws Exception {
        RemoteUaaController controller = new RemoteUaaController(new MockEnvironment(), new RestTemplate());
        Prompt first = new Prompt("how", "text", "How did I get here?");
        Prompt second = new Prompt("where", "password", "Where does that highway go to?");
        controller.setPrompts(Arrays.asList(first, second));

        MockMvc mockMvc = getMockMvc(controller);

        mockMvc.perform(get("/login").accept(TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(model().attribute("createAccountLink", "/create_account"));
    }

    @Test
    public void testCustomSignupLink() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("links.signup", "http://www.example.com/signup");

        RemoteUaaController controller = new RemoteUaaController(environment, new RestTemplate());
        Prompt first = new Prompt("how", "text", "How did I get here?");
        Prompt second = new Prompt("where", "password", "Where does that highway go to?");
        controller.setPrompts(Arrays.asList(first, second));

        MockMvc mockMvc = getMockMvc(controller);

        mockMvc.perform(get("/login").accept(TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(model().attribute("createAccountLink", "http://www.example.com/signup"));
    }

    @Test
    public void testLocalSignupDisabled() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("login.selfServiceLinksEnabled", "false");

        RemoteUaaController controller = new RemoteUaaController(environment, new RestTemplate());
        Prompt first = new Prompt("how", "text", "How did I get here?");
        Prompt second = new Prompt("where", "password", "Where does that highway go to?");
        controller.setPrompts(Arrays.asList(first, second));

        MockMvc mockMvc = getMockMvc(controller);

        mockMvc.perform(get("/login").accept(TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(model().attribute("createAccountLink", nullValue()));
    }

    @Test
    public void testCustomSignupLinkWithLocalSignupDisabled() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("login.selfServiceLinksEnabled", "false");
        environment.setProperty("links.signup", "http://www.example.com/signup");
        environment.setProperty("links.passwd", "http://www.example.com/passwd");

        RemoteUaaController controller = new RemoteUaaController(environment, new RestTemplate());
        Prompt first = new Prompt("how", "text", "How did I get here?");
        Prompt second = new Prompt("where", "password", "Where does that highway go to?");
        controller.setPrompts(Arrays.asList(first, second));

        MockMvc mockMvc = getMockMvc(controller);

        mockMvc.perform(get("/login").accept(TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(model().attribute("createAccountLink", nullValue()))
                .andExpect(model().attribute("forgotPasswordLink", nullValue()));
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
