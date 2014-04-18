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

import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

public class EmailResetPasswordServiceTests {

    private SimpleSmtpServer smtpServer;
    private EmailResetPasswordService emailResetPasswordService;
    private MockRestServiceServer mockUaaServer;
    private UriComponentsBuilder uriComponentsBuilder;

    @Before
    public void setUp() throws Exception {
        smtpServer = SimpleSmtpServer.start(2525);
        RestTemplate uaaTemplate = new RestTemplate();
        mockUaaServer = MockRestServiceServer.createServer(uaaTemplate);
        uriComponentsBuilder = UriComponentsBuilder.fromUriString("http://login.example.com/login");
        emailResetPasswordService = new EmailResetPasswordService(uaaTemplate, "http://uaa.example.com/uaa", "localhost", 2525, "", "");
    }

    @After
    public void tearDown() throws Exception {
        smtpServer.stop();
    }

    @Test
    public void testWhenAResetCodeIsReturnedByTheUaa() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/password_resets"))
                .andExpect(method(POST))
                .andRespond(withSuccess("the_secret_code", APPLICATION_JSON));

        emailResetPasswordService.forgotPassword(uriComponentsBuilder, "user@example.com");

        mockUaaServer.verify();

        Assert.assertEquals(1, smtpServer.getReceivedEmailSize());
        SmtpMessage message = (SmtpMessage) smtpServer.getReceivedEmail().next();
        Assert.assertEquals("user@example.com", message.getHeaderValue("To"));
        Assert.assertEquals("Click the link to reset your password <a href=\"http://login.example.com/login/reset_password?code=the_secret_code&email=user@example.com\">Reset Password</a>", message.getBody());
    }

    @Test
    public void testWhenTheCodeIsDenied() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/password_resets"))
                .andExpect(method(POST))
                .andRespond(withBadRequest());

        emailResetPasswordService.forgotPassword(uriComponentsBuilder, "user@example.com");

        mockUaaServer.verify();

        Assert.assertEquals(0, smtpServer.getReceivedEmailSize());
    }

    @Test
    public void testChangingAPassword() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/password_change"))
                .andExpect(method(POST))
                .andExpect(jsonPath("$.code").value("secret_code"))
                .andExpect(jsonPath("$.new_password").value("new_secret"))
                .andRespond(withSuccess("userman", APPLICATION_JSON));

        String username = emailResetPasswordService.resetPassword("secret_code", "new_secret");

        mockUaaServer.verify();

        Assert.assertEquals("userman", username);
    }
}
