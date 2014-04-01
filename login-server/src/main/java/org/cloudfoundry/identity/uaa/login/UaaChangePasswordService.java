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

import org.cloudfoundry.identity.uaa.message.PasswordChangeRequest;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class UaaChangePasswordService implements ChangePasswordService {

    private final RestTemplate authorizationTemplate;
    private final RestTemplate restTemplate;
    private final String uaaBaseUrl;

    public UaaChangePasswordService(RestTemplate authorizationTemplate, RestTemplate restTemplate, String uaaBaseUrl) {
        this.authorizationTemplate = authorizationTemplate;
        this.restTemplate = restTemplate;
        this.uaaBaseUrl = uaaBaseUrl;
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {

        String getUsersUri = "/Users?filter={filter}&attributes=id";
        Map<String, String> getUrlVariables = new HashMap<String, String>(1);
        getUrlVariables.put("filter", "userName eq '" + username + "'");

        String userId = authorizationTemplate.getForObject(uaaBaseUrl + getUsersUri, String.class, getUrlVariables);

        String changePasswordUri = "/Users/{userId}/password";
        Map<String, String> putUrlVariables = new HashMap<String, String>(1);
        putUrlVariables.put("userId", userId);

        PasswordChangeRequest passwordChangeRequest = new PasswordChangeRequest();
        passwordChangeRequest.setOldPassword(currentPassword);
        passwordChangeRequest.setPassword(newPassword);

        restTemplate.put(uaaBaseUrl + changePasswordUri, passwordChangeRequest, putUrlVariables);
    }
}
