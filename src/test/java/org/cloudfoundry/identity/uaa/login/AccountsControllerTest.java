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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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
                .andExpect(view().name("accounts/new"));
    }

    @Test
    public void testSendActivationEmail() throws Exception {
        mockMvc.perform(post("/accounts").param("email", "user1@example.com"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("email_sent?code=activation"));

        Mockito.verify(accountCreationService).beginActivation("user1@example.com");
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
