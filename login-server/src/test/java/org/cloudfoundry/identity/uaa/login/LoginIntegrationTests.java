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

import static org.hamcrest.Matchers.hasEntry;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.cloudfoundry.identity.uaa.login.test.DefaultTestConfig;
import org.cloudfoundry.identity.uaa.login.test.IntegrationTestContextLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = DefaultTestConfig.class, loader = IntegrationTestContextLoader.class)
public class LoginIntegrationTests {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    FilterChainProxy filterChainProxy;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(filterChainProxy)
                .build();
    }

    @Test
    public void testLogin() throws Exception {
        mockMvc.perform(get("/login"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("login"))
                        .andExpect(model().attribute("links", hasEntry("home", "https://console.10.244.0.34.xip.io")))
                        .andExpect(model().attribute("links",
                                        hasEntry("passwd", "https://console.10.244.0.34.xip.io/password_resets/new")))
                        .andExpect(model().attribute("links",
                                        hasEntry("register", "https://console.10.244.0.34.xip.io/register")))
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
