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
package org.cloudfoundry.identity.uaa.login.feature;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import org.cloudfoundry.identity.uaa.login.test.DefaultIntegrationTestConfig;
import org.cloudfoundry.identity.uaa.login.test.IntegrationTestRule;
import org.cloudfoundry.identity.uaa.login.test.TestClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.test.TestAccounts;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.security.SecureRandom;
import java.util.Iterator;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class CreateAccountIT {

    @Autowired
    TestAccounts testAccounts;
    
    @Autowired @Rule
    public IntegrationTestRule integrationTestRule;

    @Autowired
    WebDriver webDriver;

    @Autowired
    SimpleSmtpServer simpleSmtpServer;

    @Autowired
    TestClient testClient;

    @Value("${integration.test.base_url}")
    String baseUrl;

    @Value("${integration.test.app_url}")
    String appUrl;

    @Before
    public void setUp() {
        webDriver.get(baseUrl + "/logout.do");
    }

    @Test
    public void testUserInitiatedSignup() throws Exception {
        String userEmail = "user" + new SecureRandom().nextInt() + "@example.com";

        webDriver.get(baseUrl + "/");
        webDriver.findElement(By.xpath("//*[text()='Create account']")).click();

        Assert.assertEquals("Create Account", webDriver.findElement(By.tagName("h1")).getText());

        int receivedEmailSize = simpleSmtpServer.getReceivedEmailSize();

        webDriver.findElement(By.name("email")).sendKeys(userEmail);
        webDriver.findElement(By.xpath("//input[@value='Send activation email']")).click();

        Assert.assertEquals(receivedEmailSize + 1, simpleSmtpServer.getReceivedEmailSize());
        Iterator receivedEmail = simpleSmtpServer.getReceivedEmail();
        SmtpMessage message = (SmtpMessage) receivedEmail.next();
        receivedEmail.remove();
        Assert.assertEquals(userEmail, message.getHeaderValue("To"));
        Assert.assertThat(message.getBody(), containsString("Activate your account"));

        Assert.assertEquals("Check your email for an account activation link.", webDriver.findElement(By.cssSelector(".instructions-sent")).getText());

        String link = testClient.extractLink(message.getBody());
        webDriver.get(link);

        Assert.assertEquals("Activate Your Account", webDriver.findElement(By.tagName("h1")).getText());

        webDriver.findElement(By.name("password")).sendKeys("secret");
        webDriver.findElement(By.name("password_confirmation")).sendKeys("secret");

        webDriver.findElement(By.xpath("//input[@value='Activate']")).click();

        Assert.assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Where to?"));

        webDriver.findElement(By.xpath("//*[text()='"+userEmail+"']")).click();
        webDriver.findElement(By.linkText("Sign Out")).click();

        webDriver.findElement(By.name("username")).sendKeys(userEmail);
        webDriver.findElement(By.name("password")).sendKeys("secret");
        webDriver.findElement(By.xpath("//input[@value='Sign in']")).click();

        Assert.assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Where to?"));
    }

    @Test
    public void testClientInitiatedSignup() throws Exception {
        String userEmail = "user" + new SecureRandom().nextInt() + "@example.com";

        webDriver.get(baseUrl + "/accounts/new?client_id=app");

        Assert.assertEquals("Create Account", webDriver.findElement(By.tagName("h1")).getText());

        int receivedEmailSize = simpleSmtpServer.getReceivedEmailSize();

        webDriver.findElement(By.name("email")).sendKeys(userEmail);
        webDriver.findElement(By.xpath("//input[@value='Send activation email']")).click();

        Assert.assertEquals(receivedEmailSize + 1, simpleSmtpServer.getReceivedEmailSize());
        Iterator receivedEmail = simpleSmtpServer.getReceivedEmail();
        SmtpMessage message = (SmtpMessage) receivedEmail.next();
        receivedEmail.remove();
        Assert.assertEquals(userEmail, message.getHeaderValue("To"));
        Assert.assertThat(message.getBody(), containsString("Activate your account"));

        Assert.assertEquals("Check your email for an account activation link.", webDriver.findElement(By.cssSelector(".instructions-sent")).getText());

        String link = testClient.extractLink(message.getBody());
        webDriver.get(link);

        Assert.assertEquals("Activate Your Account", webDriver.findElement(By.tagName("h1")).getText());

        webDriver.findElement(By.name("password")).sendKeys("secret");
        webDriver.findElement(By.name("password_confirmation")).sendKeys("secret");

        webDriver.findElement(By.xpath("//input[@value='Activate']")).click();

        Assert.assertThat(webDriver.getCurrentUrl(), equalTo(appUrl));
    }
}
