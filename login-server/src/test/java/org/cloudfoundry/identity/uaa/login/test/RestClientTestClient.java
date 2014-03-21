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

import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class RestClientTestClient implements TestClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestClientTestClient(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public String getOAuthAccessToken(String username, String password, String grantType, String scope) {
        HttpHeaders headers = new HttpHeaders();
        String basicDigestHeaderValue = "Basic "
                + new String(Base64.encodeBase64((username + ":" + password).getBytes()));
        headers.add("Authorization", basicDigestHeaderValue);

        MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<String, String>();
        postParameters.add("grant_type", grantType);
        postParameters.add("client_id", username);
        postParameters.add("scope", scope);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<MultiValueMap<String, String>>(postParameters, headers);

        ResponseEntity<Map> exchange = restTemplate.exchange(baseUrl + "/oauth/token", HttpMethod.POST, requestEntity, Map.class);

        return exchange.getBody().get("access_token").toString();
    }
}
