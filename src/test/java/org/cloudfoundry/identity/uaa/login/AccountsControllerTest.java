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

import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = AccountsControllerTest.ContextConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AccountsControllerTest {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    AccountCreationService accountCreationService;

    private MockMvc mockMvc;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();
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
        Mockito.when(accountCreationService.completeActivation("expiring_code", "secret"))
            .thenReturn(new AccountCreationService.Account("newly-created-user-id", "username"));

        MockHttpServletRequestBuilder post = post("/accounts")
                .param("email", "user@example.com")
                .param("code", "expiring_code")
                .param("password", "secret")
                .param("password_confirmation", "secret");

        mockMvc.perform(post)
                .andExpect(status().isFound())
                .andExpect(model().attributeDoesNotExist("message_code"))
                .andExpect(redirectedUrl("home"));
    }

    @Test
    public void testCreateAccountWithFormValidationFailure() throws Exception {
        MockHttpServletRequestBuilder post = post("/accounts")
                .param("email", "user@example.com")
                .param("code", "expiring_code")
                .param("password", "secret")
                .param("password_confirmation", "not_secret");

        mockMvc.perform(post)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(model().attribute("message_code", "form_error"))
                .andExpect(view().name("accounts/new"))
                .andExpect(xpath("//*[@class='error-message']").string("Passwords must match and not be empty."));

        Mockito.verifyZeroInteractions(accountCreationService);
    }

    @Test
    public void testCreateAccountWithExpiredActivationCode() throws Exception {
        Mockito.when(accountCreationService.completeActivation("expired_code", "secret"))
                .thenThrow(new HttpClientErrorException(BAD_REQUEST));

        MockHttpServletRequestBuilder post = post("/accounts")
                .param("email", "user@example.com")
                .param("code", "expired_code")
                .param("password", "secret")
                .param("password_confirmation", "secret");

        mockMvc.perform(post)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(model().attribute("message_code", "code_expired"))
                .andExpect(view().name("accounts/new"))
                .andExpect(xpath("//*[@class='error-message']").string("Your activation code has expired. Please request another."));
    }

    @Test
    public void testCreateAccountWithExistingUser() throws Exception {
        Mockito.when(accountCreationService.completeActivation("expiring_code", "secret"))
                .thenThrow(new HttpClientErrorException(CONFLICT));

        MockHttpServletRequestBuilder post = post("/accounts")
                .param("email", "user@example.com")
                .param("code", "expiring_code")
                .param("password", "secret")
                .param("password_confirmation", "secret");

        mockMvc.perform(post)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(model().attribute("message_code", "email_already_taken"))
                .andExpect(view().name("accounts/new"))
                .andExpect(xpath("//*[@class='error-message']").string("You have already signed up. Please use the forgot password link from the login page."));
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
        public ResourceBundleMessageSource messageSource() {
            ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
            resourceBundleMessageSource.setBasename("messages");
            return resourceBundleMessageSource;
        }

        @Bean
        AccountCreationService accountCreationService() {
            return Mockito.mock(AccountCreationService.class);
        }

        @Bean
        AccountsController accountsController(AccountCreationService accountCreationService) {
            return new AccountsController(accountCreationService);
        }
    }
}
