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

package org.cloudfoundry.identity.uaa.login.test;


import static org.junit.Assert.fail;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.HashMap;
import java.util.List;

import org.cloudfoundry.identity.uaa.login.NotificationsBootstrap;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class NotificationsBootstrapTest {

    private NotificationsBootstrap bootstrap;
    private RestTemplate notificationsTemplate;
    private MockRestServiceServer mockNotificationsServer;
    private List<HashMap<String, Object>> notifications;


    @Before
    public void setUp() throws Exception{
        String notificationsString = "{\n" +
            "    \"kinds\": [\n" +
            "        {\n" +
            "            \"id\": \"16b1d8f3-3e04-438e-9e10-2f970b53baf7\",\n" +
            "            \"description\": \"Invalid login attempt\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"id\": \"7f8e6fb5-d18a-4b01-8827-c49fc9bc5b0b\",\n" +
            "            \"description\": \"Forgot password\",\n" +
            "            \"critical\": true\n" +
            "        }\n" +
            "    ]\n" +
            "}";
        HashMap<String,List<HashMap<String,Object>>> result =
            new ObjectMapper().readValue(notificationsString, HashMap.class);
        notifications = result.get("kinds");
        notificationsTemplate = new RestTemplate();

        bootstrap = new NotificationsBootstrap(notifications, "http://notifications.example.com/notifications", notificationsTemplate);
        bootstrap.setNotificationsTemplate(notificationsTemplate);
        bootstrap.setNotifications(notifications);
    }

    @Test
    public void testRegisterNotifications() throws Exception {
        mockNotificationsServer = MockRestServiceServer.createServer(notificationsTemplate);
        mockNotificationsServer.expect(requestTo("http://notifications.example.com/notifications/registration"))
            .andExpect(method(PUT))
            .andExpect(jsonPath("$.source_description").value("CF_Identity"))
            .andRespond(withSuccess());

        bootstrap.afterPropertiesSet();

        mockNotificationsServer.verify();
    }

    @Test
    public void testRegisterNotificationsFailure() throws Exception {
        try {
            bootstrap.afterPropertiesSet();
        } catch (ResourceAccessException e) {
            fail("NotificationsBootstrap could not be created because notifications server is down");
        }
    }
}

