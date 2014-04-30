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

import org.cloudfoundry.identity.uaa.login.test.DefaultIntegrationTestConfig;
import org.cloudfoundry.identity.uaa.login.test.TestClient;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestOperations;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class AutologinIT {

    @Autowired
    WebDriver webDriver;

    @Value("${integration.test.base_url}")
    String baseUrl;

    @Value("${integration.test.app_url}")
    String appUrl;

    @Autowired
    RestOperations restOperations;

    @Autowired
    TestClient testClient;

    @Test
    public void testAutologin() throws Exception {
        webDriver.get(baseUrl + "/logout.do");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", testClient.getBasicAuthHeaderValue("app", "appclientsecret"));

        Map<String, String> requestBody = new HashMap<String, String>();
        requestBody.put("username", "marissa");
        requestBody.put("password", "koala");

        ResponseEntity<Map> autologinResponseEntity = restOperations.exchange(baseUrl + "/autologin",
                HttpMethod.POST,
                new HttpEntity<Map<String, String>>(requestBody, headers),
                Map.class);
        String autologinCode = (String) autologinResponseEntity.getBody().get("code");

        String authorizeUrl = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/oauth/authorize")
                .queryParam("redirect_uri", appUrl)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid")
                .queryParam("client_id", "app")
                .queryParam("code", autologinCode)
                .build().toUriString();

        webDriver.get(authorizeUrl);

        webDriver.get(baseUrl);

        Assert.assertEquals("marissa", webDriver.findElement(By.cssSelector(".header .nav")).getText());
    }
}
