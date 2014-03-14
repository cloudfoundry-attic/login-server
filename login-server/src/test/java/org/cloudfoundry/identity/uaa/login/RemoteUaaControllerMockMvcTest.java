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

import org.junit.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

public class RemoteUaaControllerMockMvcTest {

    @Test
    public void testLoginWithoutAnalytics() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        MockMvc mockMvc = getMockMvc(new RemoteUaaController(environment));

        mockMvc.perform(get("/login"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("login"))
                        .andExpect(model().attributeDoesNotExist("analytics"));
    }

    @Test
    public void testLoginWithAnalytics() throws Exception {
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty("analytics.code", "secret_code");
        environment.setProperty("analytics.domain", "example.com");
        MockMvc mockMvc = getMockMvc(new RemoteUaaController(environment));

        mockMvc.perform(get("/login"))
                        .andExpect(status().isOk())
                        .andExpect(view().name("login"))
                        .andExpect(model().attribute("analytics", hasEntry("code", "secret_code")))
                        .andExpect(model().attribute("analytics", hasEntry("domain", "example.com")));
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
