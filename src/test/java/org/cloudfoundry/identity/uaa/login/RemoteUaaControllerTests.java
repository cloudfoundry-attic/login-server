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

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.client.RestOperations;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author Dave Syer
 *
 */
public class RemoteUaaControllerTests {

	private RemoteUaaController controller = new RemoteUaaController();
	
	private MockHttpServletRequest request = new MockHttpServletRequest();
	
	private Map<String,Object> model = new HashMap<String, Object>();
	
	private HttpHeaders headers = new HttpHeaders();
	
	private Principal principal = new UsernamePasswordAuthenticationToken("username", "<NONE>");
	
	private Map<String,String> parameters = new HashMap<String, String>();
	
	private RestOperations authorizationTemplate = Mockito.mock(RestOperations.class);
	
	public RemoteUaaControllerTests() {
		controller.setAuthorizationTemplate(authorizationTemplate);
	}
	
	@Test
	public void testApprovalNeeded() throws Exception {
		setResponse(Collections.<String,Object>singletonMap("options", "{}"), null, HttpStatus.OK);
		ModelAndView result = controller.startAuthorization(request, parameters, model, headers, principal);
		assertEquals("access_confirmation", result.getViewName());
	}
	
	@Test
	public void testCookieSentWithoutAttributes() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Set-Cookie", "JSESSIONID=FOO; Path=/; HttpOnly");
		setResponse(Collections.<String,Object>singletonMap("options", "{}"), responseHeaders , HttpStatus.OK);
		ModelAndView result = controller.startAuthorization(request, parameters, model, headers, principal);
		assertEquals("access_confirmation", result.getViewName());
		assertEquals("JSESSIONID=FOO", model.get("cookie"));
	}
	
	@Test
	public void testMultipleCookies() throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.add("Set-Cookie", "__VCAP_ID__=BAR");
		responseHeaders.add("Set-Cookie", "JSESSIONID=FOO; Path=/; HttpOnly");
		setResponse(Collections.<String,Object>singletonMap("options", "{}"), responseHeaders , HttpStatus.OK);
		ModelAndView result = controller.startAuthorization(request, parameters, model, headers, principal);
		assertEquals("access_confirmation", result.getViewName());
		assertEquals("__VCAP_ID__=BAR;JSESSIONID=FOO", model.get("cookie"));
	}
	
	private void setResponse( Map<String, Object> body, HttpHeaders headers, HttpStatus status) {
		if (headers==null) {
			headers = new HttpHeaders();
		}
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = new ResponseEntity<Map>(body, headers, status);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		ResponseEntity<Map> exchange = authorizationTemplate.exchange(Matchers.anyString(), Matchers.any(HttpMethod.class), Matchers.any(HttpEntity.class), Matchers.any(Class.class));
		Mockito.when(exchange).thenReturn(response);		
	}

}
