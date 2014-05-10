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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dave Syer
 */
public class RemoteUaaControllerTests {

    private RemoteUaaController controller = new RemoteUaaController(new MockEnvironment(), new RestTemplate());

    private MockHttpServletRequest request = new MockHttpServletRequest();

    private Map<String, Object> model = new HashMap<String, Object>();

    private HttpHeaders headers = new HttpHeaders();

    private Principal principal = new UsernamePasswordAuthenticationToken("username", "<NONE>");

    private Map<String, String> parameters = new HashMap<String, String>();

    private RestOperations authorizationTemplate = Mockito.mock(RestOperations.class);

    public RemoteUaaControllerTests() {
        controller.setAuthorizationTemplate(authorizationTemplate);
    }

    @Test
    public void testApprovalNeeded() throws Exception {
        setResponse(Collections.<String, Object> singletonMap("options", "{}"), null, HttpStatus.OK);
        ModelAndView result = controller.startAuthorization(request, parameters, model, headers, principal);
        assertEquals("access_confirmation", result.getViewName());
    }

    @Test
    public void testCookieSentWithoutAttributes() throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Set-Cookie", "JSESSIONID=FOO; Path=/; HttpOnly");
        setResponse(Collections.<String, Object> singletonMap("options", "{}"), responseHeaders, HttpStatus.OK);
        ModelAndView result = controller.startAuthorization(request, parameters, model, headers, principal);
        assertEquals("access_confirmation", result.getViewName());
        assertEquals("JSESSIONID=FOO", model.get("cookie"));
    }

    @Test
    public void testMultipleCookies() throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Set-Cookie", "__VCAP_ID__=BAR");
        responseHeaders.add("Set-Cookie", "JSESSIONID=FOO; Path=/; HttpOnly");
        setResponse(Collections.<String, Object>singletonMap("options", "{}"), responseHeaders, HttpStatus.OK);
        ModelAndView result = controller.startAuthorization(request, parameters, model, headers, principal);
        assertEquals("access_confirmation", result.getViewName());
        assertEquals("__VCAP_ID__=BAR;JSESSIONID=FOO", model.get("cookie"));
    }

    @Test
    public void testRedirectUri() throws Exception {
        Map<String, String> requestParams = new HashMap<String, String>();
        requestParams.put("redirect-uri", "http://www.google.com");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Location", "http://www.example.com");
        setResponse(null, responseHeaders, HttpStatus.FOUND);

        ModelAndView result = controller.startAuthorization(request, requestParams, model, headers, principal);

        RedirectView view = (RedirectView) result.getView();
        assertTrue(view.toString().contains("http://www.example.com"));

        ArgumentCaptor<HttpEntity> uaaRequest = ArgumentCaptor.forClass(HttpEntity.class);
        Mockito.verify(authorizationTemplate).exchange(Matchers.anyString(), Matchers.any(HttpMethod.class), uaaRequest.capture(), Matchers.any(Class.class));
        MultiValueMap<String, String> uaaRequestBody = (MultiValueMap<String, String>) uaaRequest.getValue().getBody();
        String requestUri = uaaRequestBody.getFirst("redirect-uri");
        assertTrue(requestUri.contains("http://www.google.com"));
        assertFalse(requestUri.contains("http://http://www.google.com"));
    }

    @Test
    public void testRedirectUriWithMissingProtocol() throws Exception {
        Map<String, String> requestParams = new HashMap<String, String>();
        requestParams.put("redirect-uri", "www.google.com");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Location", "http://www.example.com");
        setResponse(null, responseHeaders, HttpStatus.FOUND);

        controller.startAuthorization(request, requestParams, model, headers, principal);

        ArgumentCaptor<HttpEntity> uaaRequest = ArgumentCaptor.forClass(HttpEntity.class);
        Mockito.verify(authorizationTemplate).exchange(Matchers.anyString(), Matchers.any(HttpMethod.class), uaaRequest.capture(), Matchers.any(Class.class));
        MultiValueMap<String, String> uaaRequestBody = (MultiValueMap<String, String>) uaaRequest.getValue().getBody();
        assertTrue(uaaRequestBody.getFirst("redirect-uri").contains("http://www.google.com"));
    }

    @Test
    public void testProtocolRelativeRedirectUri() throws Exception {
        Map<String, String> requestParams = new HashMap<String, String>();
        requestParams.put("redirect-uri", "//www.google.com");

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Location", "http://www.example.com");
        setResponse(null, responseHeaders, HttpStatus.FOUND);

        controller.startAuthorization(request, requestParams, model, headers, principal);

        ArgumentCaptor<HttpEntity> uaaRequest = ArgumentCaptor.forClass(HttpEntity.class);
        Mockito.verify(authorizationTemplate).exchange(Matchers.anyString(), Matchers.any(HttpMethod.class), uaaRequest.capture(), Matchers.any(Class.class));
        MultiValueMap<String, String> uaaRequestBody = (MultiValueMap<String, String>) uaaRequest.getValue().getBody();
        String requestUri = uaaRequestBody.getFirst("redirect-uri");
        assertTrue(requestUri.contains("www.google.com"));
        assertFalse(requestUri.contains("http://www.google.com"));
    }

    private void setResponse(Map<String, Object> body, HttpHeaders headers, HttpStatus status) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response = new ResponseEntity<Map>(body, headers, status);
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ResponseEntity<Map> exchange = authorizationTemplate.exchange(Matchers.anyString(),
                        Matchers.any(HttpMethod.class), Matchers.any(HttpEntity.class), Matchers.any(Class.class));
        Mockito.when(exchange).thenReturn(response);
    }

}
