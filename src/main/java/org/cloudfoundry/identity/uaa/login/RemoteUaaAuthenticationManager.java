/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.cloudfoundry.identity.uaa.login;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * An authentication manager that can be used to login to a remote UAA service with username and password credentials,
 * without the local server needing to know anything about the user accounts. The request is handled by the UAA's
 * RemoteAuhenticationEndpoint and success or failure is determined by the response code.
 *
 * @author Dave Syer
 * @author Luke Taylor
 *
 */
public class RemoteUaaAuthenticationManager implements AuthenticationManager {

	private final Log logger = LogFactory.getLog(getClass());

	private RestOperations restTemplate = new RestTemplate();

	private static String DEFAULT_LOGIN_URL = "http://uaa.cloudfoundry.com/authenticate";

	private String loginUrl = DEFAULT_LOGIN_URL;

	/**
	 * @param loginUrl the login url to set
	 */
	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

	/**
	 * @param restTemplate a rest template to use
	 */
	public void setRestTemplate(RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

	public RemoteUaaAuthenticationManager() {
		RestTemplate restTemplate = new RestTemplate();
		// The default java.net client doesn't allow you to handle 4xx responses
		restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			protected boolean hasError(HttpStatus statusCode) {
				return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
			}
		});
		this.restTemplate = restTemplate;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {
		String username = authentication.getName();
		String password = (String) authentication.getCredentials();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

		MultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
		parameters.set("username", username);
		parameters.set("password", password);

		ResponseEntity<Map> response = restTemplate.exchange(loginUrl, HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, Object>>(parameters, headers), Map.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			String userFromUaa = (String) response.getBody().get("username");

			if (userFromUaa.equals(userFromUaa)) {
				logger.info("Successful authentication request for " + authentication.getName());
				return new UsernamePasswordAuthenticationToken(username, null, UaaAuthority.USER_AUTHORITIES);
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

}
