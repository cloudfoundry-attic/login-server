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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

public class EmailResetPasswordServiceTests {

    private SimpleSmtpServer smtpServer;
    private RestTemplate uaaTemplate;
    private EmailResetPasswordService emailResetPasswordService;

    @Before
    public void setUp() throws Exception {
        smtpServer = SimpleSmtpServer.start(2525);
        uaaTemplate = Mockito.mock(RestTemplate.class);
        emailResetPasswordService = new EmailResetPasswordService(uaaTemplate, "http://uaa.example.com/uaa", "localhost", 2525, "", "");
    }

    @After
    public void tearDown() throws Exception {
        smtpServer.stop();
    }

    @Test
    public void testWhenAResetCodeIsReturnedByTheUaa() throws Exception {
        Mockito.when(uaaTemplate.postForObject("http://uaa.example.com/uaa/password_resets", "user@example.com", String.class)).thenReturn("the_secret_code");

        emailResetPasswordService.forgotPassword("user@example.com");

        Assert.assertEquals(1, smtpServer.getReceivedEmailSize());
        SmtpMessage message = (SmtpMessage) smtpServer.getReceivedEmail().next();
        Assert.assertEquals("user@example.com", message.getHeaderValue("To"));
        Assert.assertEquals("Click the link to reset your password <a href=\"http://localhost:8080/login/reset_password?code=the_secret_code\">Reset Password</a>", message.getBody());
    }

    @Test
    public void testWhenTheCodeIsDenied() throws Exception {
        Mockito.when(uaaTemplate.postForObject("http://uaa.example.com/uaa/password_resets", "user@example.com", String.class)).thenThrow(new RestClientException("no code for you"));

        emailResetPasswordService.forgotPassword("user@example.com");

        Assert.assertEquals(0, smtpServer.getReceivedEmailSize());
    }

    @Test
    @Ignore
    public void testChangingAPassword() throws Exception {

    }
}
