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
import org.cloudfoundry.identity.uaa.login.test.IntegrationTestRule;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.test.TestAccounts;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class CreateAccountIT {

    @Autowired
    TestAccounts testAccounts;
    
    @Autowired @Rule
    public IntegrationTestRule integrationTestRule;

    @Autowired
    WebDriver webDriver;

    @Value("${integration.test.base_url}")
    String baseUrl;

    @Before
    public void setUp() {
        webDriver.get(baseUrl + "/logout.do");
    }

    @Test
    public void tiles() throws Exception {
        webDriver.get(baseUrl + "/");
        webDriver.findElement(By.xpath("//*[text()='Create account']")).click();

        List<WebElement> tiles = webDriver.findElements(By.cssSelector(".tiles li a"));
        assertEquals(3, tiles.size());

        assertEquals("Pivotal Network", tiles.get(0).getText());
        assertEquals("https://network.gopivotal.com/registrations/new", tiles.get(0).getAttribute("href"));
        assertEquals("url(http://localhost:8080/login/resources/pivotal/images/network-logo-gray.png)", tiles.get(0).getCssValue("background-image"));

        assertEquals("Pivotal Web Services", tiles.get(1).getText());

        assertEquals("Pivotal Partners", tiles.get(2).getText());
    }
}
