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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.cloudfoundry.identity.uaa.login.test.DefaultTestConfig;
import org.cloudfoundry.identity.uaa.login.test.DefaultTestConfigContextLoader;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = DefaultTestConfig.class, loader = DefaultTestConfigContextLoader.class)
public class ResetPasswordControllerIntegrationTests {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    FilterChainProxy springSecurityFilterChain;
    
    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        Assume.assumeFalse("Reset password functionality is disabled by the saml profile", Arrays.asList(webApplicationContext.getEnvironment().getActiveProfiles()).contains("saml"));

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilter(springSecurityFilterChain)
                .build();
    }

    @Test
    public void testForgotPasswordPage() throws Exception {
        mockMvc.perform(get("/forgot_password"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot_password"));
    }

    @Test
    public void testResetPasswordPage() throws Exception {
        // any code will render the page, but only a valid code actually sets a password

        MockHttpServletRequestBuilder get = get("/reset_password")
                .param("code", "any_code_will_show_the_page")
                .param("email", "user@example.com");

        mockMvc.perform(get)
                .andExpect(status().isOk())
                .andExpect(view().name("reset_password"));
    }
}
