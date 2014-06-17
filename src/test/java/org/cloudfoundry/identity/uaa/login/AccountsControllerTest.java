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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

public class AccountsControllerTest {

    private MockMvc mockMvc;
    private AccountCreationService accountCreationService;

    @Before
    public void setUp() throws Exception {
        accountCreationService = Mockito.mock(AccountCreationService.class);

        mockMvc = getStandaloneMockMvc(new AccountsController(accountCreationService));
    }

    @Test
    public void testNewAccountPage() throws Exception {
        mockMvc.perform(get("/accounts/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/new_activation_email"));
    }

    @Test
    public void testSendActivationEmail() throws Exception {
        mockMvc.perform(post("/accounts").param("email", "user1@example.com"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("email_sent?code=activation"));

        Mockito.verify(accountCreationService).beginActivation("user1@example.com");
    }

    @Test
    public void testNewAccountPageWithActivationCode() throws Exception {
        mockMvc.perform(get("/accounts/new").param("code", "expiring_code").param("email", "user@example.com"))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/new"));
    }

    @Test
    public void testCreateAccount() throws Exception {
        Mockito.when(accountCreationService.completeActivation("expiring_code", "secret")).thenReturn("username");

        MockHttpServletRequestBuilder post = post("/accounts")
                .param("code", "expiring_code")
                .param("password", "secret")
                .param("password_confirmation", "secret");

        mockMvc.perform(post)
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("home"));
    }

    @Test
    @Ignore
    public void testCreateAccountWithExpiredActivationCode() throws Exception {
        //TODO: simulate expired code on uaa

        MockHttpServletRequestBuilder post = post("/accounts/new")
                .param("code", "expired_code")
                .param("password", "secret")
                .param("password_confirmation", "secret");

        mockMvc.perform(post)
                .andExpect(status().isBadRequest())
                .andExpect(flash().attribute("message_code", "code_expired"))
                .andExpect(view().name("accounts/new"));
    }

    private MockMvc getStandaloneMockMvc(AccountsController controller) {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/jsp/pivotal");
        viewResolver.setSuffix(".jsp");
        return MockMvcBuilders
                .standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }
}
