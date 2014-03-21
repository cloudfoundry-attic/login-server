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

import static org.hamcrest.Matchers.containsString;

import org.cloudfoundry.identity.uaa.login.test.DefaultIntegrationTestConfig;
import org.cloudfoundry.identity.uaa.login.test.TestClient;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class ResetPasswordIT {

    @Autowired
    WebDriver webDriver;

    @Autowired
    SimpleSmtpServer simpleSmtpServer;

    @Autowired
    TestClient testClient;

    @Autowired
    RestTemplate restTemplate;

    @Value("${integration.test.base_url}")
    String baseUrl;

    @Value("${integration.test.uaa_url}")
    String uaaUrl;

    @Before
    public void setUp() throws Exception {
        String adminAccessToken = testClient.getOAuthAccessToken("admin", "adminsecret", "client_credentials", "clients.read clients.write clients.secret");
        createScimClient(adminAccessToken);
        String scimAccessToken = testClient.getOAuthAccessToken("scim", "scimsecret", "client_credentials", "scim.read scim.write password.write");
        createUser(scimAccessToken);
    }

    @Test
    public void requestingAPasswordReset() throws Exception {
        webDriver.get(baseUrl + "/login");
        Assert.assertEquals("Pivotal", webDriver.getTitle());

        webDriver.findElement(By.linkText("Forgot Password")).click();

        webDriver.findElement(By.name("email")).sendKeys("user@example.com");
        webDriver.findElement(By.xpath("//button[contains(text(),'Reset Password')]")).click();

        Assert.assertEquals(1, simpleSmtpServer.getReceivedEmailSize());
        SmtpMessage message = (SmtpMessage) simpleSmtpServer.getReceivedEmail().next();
        Assert.assertEquals("user@example.com", message.getHeaderValue("To"));
        Assert.assertThat(message.getBody(), containsString("This is a placeholder email.  We cannot support resetting of passwords just yet.  Sorry for the ruse."));

        Assert.assertEquals("An email has been sent with password reset instructions.", webDriver.findElement(By.cssSelector(".flash")).getText());
    }

    private void createScimClient(String adminAccessToken) throws Exception {
        restfulCreate(
                adminAccessToken,
                "{\"scope\":[\"uaa.none\"],\"client_id\":\"scim\",\"client_secret\":\"scimsecret\",\"resource_ids\":[\"oauth\"],\"authorized_grant_types\":[\"client_credentials\"],\"authorities\":[\"password.write\",\"scim.write\",\"scim.read\",\"oauth.approvals\"]}",
                uaaUrl + "/oauth/clients"
        );
    }

    private void createUser(String scimAccessToken) throws Exception {
        ScimUser user = new ScimUser();
        user.setUserName("JOE");
        user.setName(new ScimUser.Name("Joe", "User"));
        user.addEmail("user@example.com");

        restfulCreate(
                scimAccessToken,
                new ObjectMapper().writeValueAsString(user),
                uaaUrl + "/Users"
        );
    }

    private void restfulCreate(String adminAccessToken, String json, String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + adminAccessToken);
        headers.add("Accept", "application/json");
        headers.add("Content-Type", "application/json");

        HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
        ResponseEntity<Void> exchange = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Void.class);
        Assert.assertEquals(HttpStatus.CREATED, exchange.getStatusCode());
    }
}
