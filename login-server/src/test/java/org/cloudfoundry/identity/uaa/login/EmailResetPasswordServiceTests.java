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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.cloudfoundry.identity.uaa.login.test.FakeJavaMailSender;
import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.spring3.SpringTemplateEngine;

import java.util.Arrays;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ThymeleafConfig.class)
public class EmailResetPasswordServiceTests {

    private EmailResetPasswordService emailResetPasswordService;
    private MockRestServiceServer mockUaaServer;
    private UriComponentsBuilder uriComponentsBuilder;
    private FakeJavaMailSender mailSender;

    @Autowired
    @Qualifier("mailTemplateEngine")
    SpringTemplateEngine templateEngine;

    @Before
    public void setUp() throws Exception {
        RestTemplate uaaTemplate = new RestTemplate();
        mockUaaServer = MockRestServiceServer.createServer(uaaTemplate);
        uriComponentsBuilder = UriComponentsBuilder.fromUriString("http://login.example.com/login");
        mailSender = new FakeJavaMailSender();
        emailResetPasswordService = new EmailResetPasswordService(templateEngine, uaaTemplate, "http://uaa.example.com/uaa", mailSender, "pivotal", "http://login.example.com/login");
    }

    @Test
    public void testWhenAResetCodeIsReturnedByTheUaa() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/password_resets"))
                .andExpect(method(POST))
                .andRespond(withSuccess("the_secret_code", APPLICATION_JSON));

        emailResetPasswordService.forgotPassword(uriComponentsBuilder, "user@example.com");

        mockUaaServer.verify();

        Assert.assertEquals(1, mailSender.getSentMessages().size());

        FakeJavaMailSender.MimeMessageWrapper messageWrapper = mailSender.getSentMessages().get(0);
        Assert.assertEquals(Arrays.asList(new InternetAddress("user@example.com")), messageWrapper.getRecipients(Message.RecipientType.TO));
        Assert.assertEquals(Arrays.asList(new InternetAddress("admin@login.example.com")), messageWrapper.getFrom());
        Assert.assertEquals("Pivotal", ((InternetAddress)messageWrapper.getFrom().get(0)).getPersonal());
        Assert.assertThat(messageWrapper.getContentString(), containsString("<a href=\"http://login.example.com/login/reset_password?code=the_secret_code&amp;email=user%40example.com\">Reset your password</a>"));
    }

    @Test
    public void testWhenTheCodeIsDenied() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/password_resets"))
                .andExpect(method(POST))
                .andRespond(withBadRequest());

        emailResetPasswordService.forgotPassword(uriComponentsBuilder, "user@example.com");

        mockUaaServer.verify();

        Assert.assertEquals(0, mailSender.getSentMessages().size());
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
