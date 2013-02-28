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

package org.cloudfoundry.identity.uaa.login.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Map;

import org.cloudfoundry.identity.uaa.login.AutologinController.AutologinRequest;
import org.cloudfoundry.identity.uaa.test.UaaTestAccounts;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Dave Syer
 *
 */
public class AutologinContollerIntegrationTests {

	@Rule
	public ServerRunning serverRunning = ServerRunning.isRunning();

	private UaaTestAccounts testAccounts = UaaTestAccounts.standard(serverRunning);

	private HttpHeaders headers = new HttpHeaders();

	@Before
	public void init() {
		AuthorizationCodeResourceDetails client = testAccounts.getDefaultAuthorizationCodeResource();
		headers.set("Authorization",
				testAccounts.getAuthorizationHeader(client.getClientId(), client.getClientSecret()));
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
	}

	@Test
	public void testGetCode() {
		AutologinRequest request = new AutologinRequest();
		request.setUsername(testAccounts.getUserName());
		request.setPassword(testAccounts.getPassword());
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = serverRunning.getRestTemplate().exchange(serverRunning.getUrl("/autologin"),
				HttpMethod.POST, new HttpEntity<AutologinRequest>(request, headers), Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) entity.getBody();
		assertNotNull(result.get("code"));
	}

	@Test
	public void testGetCodeWithFormEncodedRequest() {
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String,String> request = new LinkedMultiValueMap<String, String>();
		request.set("username",testAccounts.getUserName());
		request.set("password", testAccounts.getPassword());
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = serverRunning.getRestTemplate().exchange(serverRunning.getUrl("/autologin"),
				HttpMethod.POST, new HttpEntity<MultiValueMap>(request, headers), Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) entity.getBody();
		assertNotNull(result.get("code"));
	}

	@Test
	public void testUnauthorizedWithoutPassword() {
		AutologinRequest request = new AutologinRequest();
		request.setUsername(testAccounts.getUserName());
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = serverRunning.getRestTemplate().exchange(serverRunning.getUrl("/autologin"),
				HttpMethod.POST, new HttpEntity<AutologinRequest>(request, headers), Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) entity.getBody();
		assertNull(result.get("code"));
	}

	@Test
	public void testClientUnauthorized() {
		AutologinRequest request = new AutologinRequest();
		request.setUsername(testAccounts.getUserName());
		request.setPassword(testAccounts.getPassword());
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = serverRunning.getRestTemplate().exchange(serverRunning.getUrl("/autologin"),
				HttpMethod.POST, new HttpEntity<AutologinRequest>(request), Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> result = (Map<String, Object>) entity.getBody();
		assertNull(result.get("code"));
	}

}
