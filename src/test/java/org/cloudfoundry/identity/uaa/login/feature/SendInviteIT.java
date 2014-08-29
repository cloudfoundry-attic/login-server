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
import org.apache.commons.io.FileUtils;
import org.cloudfoundry.identity.uaa.login.test.DefaultIntegrationTestConfig;
import org.cloudfoundry.identity.uaa.login.test.IfProfileActive;
import org.cloudfoundry.identity.uaa.login.test.IntegrationTestRule;
import org.cloudfoundry.identity.uaa.login.test.TestClient;
import org.cloudfoundry.identity.uaa.test.TestProfileEnvironment;
import org.cloudfoundry.identity.uaa.test.UaaTestAccounts;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.security.oauth2.client.test.TestAccounts;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Iterator;

import static org.hamcrest.Matchers.containsString;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class SendInviteIT {

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

    private HomeIT.HomePagePerspective asOnHomePage;

    @BeforeClass
    public static void setProperties() {
        System.setProperty("login.invitationsEnabled", "true");
    }


    @Before
    public void setUp() {
        webDriver.get(baseUrl + "/logout.do");

        webDriver.get(baseUrl + "/login");
        webDriver.findElement(By.name("username")).sendKeys(testAccounts.getUserName());
        webDriver.findElement(By.name("password")).sendKeys(testAccounts.getPassword());
        webDriver.findElement(By.xpath("//input[@value='Sign in']")).click();

        asOnHomePage = new HomeIT.HomePagePerspective(webDriver, testAccounts.getUserName());
    }
    @Test
    public void testInvitationFlow() throws Exception {
        String userEmail = "user" + new SecureRandom().nextInt() + "@example.com";
        webDriver.findElement(By.xpath("//*[text()='Invite Users']")).click();

        int receivedEmailSize = simpleSmtpServer.getReceivedEmailSize();
//        File scrFile = ((TakesScreenshot) webDriver).getScreenshotAs(
//            OutputType.FILE);
//        String scrFilename = "Screenshot.png";
//        File outputFile = new File("/Users/pivotal/Downloads", scrFilename);
//        try {
//            FileUtils.copyFile(scrFile, outputFile);
//        } catch (IOException ioe) {
//        }

        webDriver.findElement(By.name("email")).sendKeys(userEmail);
        webDriver.findElement(By.xpath("//input[@value='Send invite']")).click();

        Assert.assertEquals(receivedEmailSize + 1, simpleSmtpServer.getReceivedEmailSize());
        Iterator receivedEmail = simpleSmtpServer.getReceivedEmail();
        SmtpMessage message = (SmtpMessage) receivedEmail.next();
        receivedEmail.remove();
        Assert.assertEquals(userEmail, message.getHeaderValue("To"));
        Assert.assertThat(message.getBody(), containsString("has invited you to"));

        Assert.assertEquals("Invite sent", webDriver.findElement(By.tagName("h1")).getText());

        String link = testClient.extractLink(message.getBody());
        webDriver.get(link);

        Assert.assertEquals("Create your account", webDriver.findElement(By.tagName("h1")).getText());
    }

    public static class PropertyMockingApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
//            MutablePropertySources propertySources = TestProfileEnvironment.getEnvironment().getPropertySources();
//            MockPropertySource mockEnvVars = new MockPropertySource().withProperty("login.invitationsEnabled", true);
//            propertySources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, mockEnvVars);
        }
    }
}
