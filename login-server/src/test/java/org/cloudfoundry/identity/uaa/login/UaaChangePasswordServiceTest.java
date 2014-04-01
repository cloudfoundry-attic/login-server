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

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class UaaChangePasswordServiceTest {
    private MockRestServiceServer mockUaaServer;
    private UaaChangePasswordService subject;

    @Before
    public void setUp() throws Exception {
        RestTemplate uaaTemplate = new RestTemplate();
        mockUaaServer = MockRestServiceServer.createServer(uaaTemplate);
        subject = new UaaChangePasswordService(uaaTemplate, uaaTemplate, "http://uaa.example.com/uaa");
    }

    @Test
    public void testChangePassword() throws Exception {
        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Users?filter=userName%20eq%20'the%20user%20name'&attributes=id"))
                .andExpect(method(GET))
                .andRespond(withSuccess("123", APPLICATION_JSON));

        mockUaaServer.expect(requestTo("http://uaa.example.com/uaa/Users/123/password"))
                .andExpect(method(PUT))
//                .andExpect(jsonPath("$."))
                .andRespond(withSuccess("derp derp", APPLICATION_JSON));

        subject.changePassword("the user name", "current password", "new password");

        mockUaaServer.verify();
    }
}
