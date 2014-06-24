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

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * An authentication manager that can be used to login to a remote UAA service
 * with username and password credentials,
 * without the local server needing to know anything about the user accounts.
 * The request is handled by the UAA's
 * RemoteAuhenticationEndpoint and success or failure is determined by the
 * response code.
 * 
 * @author Dave Syer
 * @author Luke Taylor
 * 
 */
public class RemoteUaaAuthenticationManager implements AuthenticationManager {

    protected final Log logger = LogFactory.getLog(getClass());

    private RestOperations restTemplate = new RestTemplate();

    private static String DEFAULT_LOGIN_URL = "http://uaa.cloudfoundry.com/authenticate";

    private String loginUrl = DEFAULT_LOGIN_URL;

    /**
     * @param loginUrl the login url to set
     */
    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }
    
    public String getLoginUrl() {
        return loginUrl;
    }

    /**
     * @param restTemplate a rest template to use
     */
    public void setRestTemplate(RestOperations restTemplate) {
        this.restTemplate = restTemplate;
        if (restTemplate instanceof RestTemplate) {
            initRestTemplateErrorHandler((RestTemplate)restTemplate);
        }
    }
    
    public RestOperations getRestTemplate() {
        return restTemplate;
    }

    public RemoteUaaAuthenticationManager() {
        setRestTemplate(new RestTemplate());
        // The default java.net client doesn't allow you to handle 4xx responses
    }

    private void initRestTemplateErrorHandler(RestTemplate restTemplate) {
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            protected boolean hasError(HttpStatus statusCode) {
                return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
            }
        });
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        HttpHeaders headers = getHeaders();

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = restTemplate.exchange(loginUrl, HttpMethod.POST,
                        new HttpEntity<Object>(getParameters(username, password), headers), Map.class);

        if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
            Authentication auth = evaluateResponse(authentication, response);
            if (auth!=null) {
                logger.info("Successful authentication request for " + authentication.getName());
                return auth;
            }
        } else if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            logger.info("Failed authentication request");
            throw new BadCredentialsException("Authentication failed");
        } else if (response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
            logger.info("Internal error from UAA. Please Check the UAA logs.");
        } else {
            logger.error("Unexpected status code " + response.getStatusCode() + " from the UAA." +
                            " Is a compatible version running?");
        }
        throw new RuntimeException("Could not authenticate with remote server");
    }
    
    protected Authentication evaluateResponse(Authentication authentication, ResponseEntity<Map> response) {
        String userFromUaa = (String) response.getBody().get("username");
        String userId = (String)response.getBody().get("user_id");
        String origin = (String)response.getBody().get(Origin.ORIGIN);
        if (userFromUaa.equalsIgnoreCase(authentication.getPrincipal().toString())) {
            if (StringUtils.hasText(userId) && StringUtils.hasText(origin)) {
                UaaPrincipal principal = new UaaPrincipal(userId, userFromUaa, null, origin, null);
                return new UsernamePasswordAuthenticationToken(principal, null, UaaAuthority.USER_AUTHORITIES);
            } else {
                return new UsernamePasswordAuthenticationToken(userFromUaa, null, UaaAuthority.USER_AUTHORITIES);
            }
        } else {
            logger.debug("Authentication username mismatch:"+userFromUaa);
            return null;
        }
    }

    protected Object getParameters(String username, String password) {
        MultiValueMap<String, Object> parameters = new LinkedMaskingMultiValueMap<String, Object>("password");
        parameters.set("username", username);
        parameters.set("password", password);
        return parameters;
    }

    protected HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
