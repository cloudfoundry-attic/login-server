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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.client.RestOperations;

/**
 * @author Dave Syer
 * @author Luke Taylor
 */
public class RemoteUaaAuthenticationManagerTests {

	private RemoteUaaAuthenticationManager authenticationManager = new RemoteUaaAuthenticationManager();

	private RestOperations restTemplate = mock(RestOperations.class);

	private HttpHeaders responseHeaders = new HttpHeaders();

	@Before
	public void start() {
		authenticationManager.setRestTemplate(restTemplate);
	}

	@Test
	public void testAuthenticate() throws Exception {
		responseHeaders.setLocation(new URI("https://uaa.cloudfoundry.com/"));
		Map<String,String> response = new HashMap<String, String>();
		response.put("username", "marissa");
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> expectedResponse = new ResponseEntity<Map>(response, responseHeaders, HttpStatus.OK);
		when(restTemplate.exchange(endsWith("/authenticate"), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
				.thenReturn(expectedResponse);
		Authentication result = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("marissa",
				"foo"));
		assertEquals("marissa", result.getName());
		assertTrue(result.isAuthenticated());
	}

}
