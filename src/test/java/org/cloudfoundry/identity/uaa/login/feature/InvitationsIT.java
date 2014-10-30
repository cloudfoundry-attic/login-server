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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class InvitationsIT {

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

    @Test
    public void testSendInvite() throws Exception {
        sendInvite(1);
    }
    
    public void sendInvite(int numUsers) throws Exception {
    	List<String> userEmailList = new ArrayList<String>();
    	for (int i = 0; i < numUsers; i++) {
	        int randomInt = new SecureRandom().nextInt();
	        userEmailList.add("user" + randomInt + "@example.com");
    	}
    	String userEmails = userEmailList.toString().replaceAll("[\\[\\]]", "");
    	
        int receivedEmailSize = simpleSmtpServer.getReceivedEmailSize();

        signIn(testAccounts.getUserName(), testAccounts.getPassword());

        webDriver.findElement(By.linkText("Invite Users")).click();
        Assert.assertEquals("Send an invite", webDriver.findElement(By.tagName("h1")).getText());

        webDriver.findElement(By.name("email")).sendKeys(userEmails);
        webDriver.findElement(By.xpath("//input[@value='Send invite']")).click();

        Assert.assertEquals("Invite sent", webDriver.findElement(By.tagName("h1")).getText());
        
        Assert.assertEquals(receivedEmailSize + numUsers, simpleSmtpServer.getReceivedEmailSize());
        for (int i = 0; i < numUsers; i++) {
        	
	        Iterator receivedEmail = simpleSmtpServer.getReceivedEmail();
	        SmtpMessage message = (SmtpMessage) receivedEmail.next();
	        receivedEmail.remove();
	        String userEmail = message.getHeaderValue("To");
	        
	        Assert.assertTrue(userEmailList.remove(userEmail));
	        Assert.assertThat(message.getBody(), containsString("Accept Invite"));
	
	        String link = testClient.extractLink(message.getBody());
	        Assert.assertTrue(link.contains("/invitations/accept"));
	        webDriver.get(link);
	
	        Assert.assertEquals("Create your account", webDriver.findElement(By.tagName("h1")).getText());
	
	        webDriver.findElement(By.name("password")).sendKeys("secret");
	        webDriver.findElement(By.name("password_confirmation")).sendKeys("secret");
	
	        webDriver.findElement(By.xpath("//input[@value='Create account']")).click();
	
	        Assert.assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Where to?"));
	
	        webDriver.findElement(By.xpath("//*[text()='"+userEmail+"']")).click();
	        webDriver.findElement(By.linkText("Sign Out")).click();
	
	        webDriver.findElement(By.name("username")).sendKeys(userEmail);
	        webDriver.findElement(By.name("password")).sendKeys("secret");
	        webDriver.findElement(By.xpath("//input[@value='Sign in']")).click();
	
	        Assert.assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Where to?"));
	        webDriver.findElement(By.xpath("//*[text()='"+userEmail+"']")).click();
	        webDriver.findElement(By.linkText("Sign Out")).click();
        }
    }
    
    @Test
    public void testSendInviteToMultipleUsers() throws Exception {
    	sendInvite(5);
    }

    private void signIn(String userName, String password) {
        webDriver.get(baseUrl + "/login");
        webDriver.findElement(By.name("username")).sendKeys(userName);
        webDriver.findElement(By.name("password")).sendKeys(password);
        webDriver.findElement(By.xpath("//input[@value='Sign in']")).click();
        Assert.assertThat(webDriver.findElement(By.cssSelector("h1")).getText(), containsString("Where to?"));
    }
}
