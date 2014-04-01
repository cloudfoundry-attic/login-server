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

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

public class ChangePasswordControllerTest {
    private MockMvc mockMvc;
    private ChangePasswordService changePasswordService;

    @Before
    public void setUp() throws Exception {
        changePasswordService = Mockito.mock(ChangePasswordService.class);
        ChangePasswordController controller = new ChangePasswordController(changePasswordService);

        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/WEB-INF/jsp");
        viewResolver.setSuffix(".jsp");
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void testChangePasswordPage() throws Exception {
        mockMvc.perform(get("/change_password"))
                .andExpect(status().isOk())
                .andExpect(view().name("change_password"));
    }

    @Test
    public void testForgotPassword() throws Exception {
        MockHttpServletRequestBuilder post = post("/change_password.do")
                .contentType(APPLICATION_FORM_URLENCODED)
                .param("current_password", "secret")
                .param("new_password", "new secret")
                .param("confirm_password", "new secret");

        mockMvc.perform(post)
                .andExpect(status().isFound())
                .andExpect(flash().attributeExists("success"));

        Mockito.verify(changePasswordService).changePassword("", "secret", "new secret");
    }
}
