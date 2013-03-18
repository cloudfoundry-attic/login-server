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

package org.cloudfoundry.identity.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.cloudfoundry.identity.uaa.util.UaaStringUtils;
import org.opensaml.saml2.core.impl.NameIDImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@Component
@SessionAttributes(value = "cookie")
public class CloudfoundryServiceProviderController {

    private final Logger log = LoggerFactory.getLogger(CloudfoundryServiceProviderController.class);
    
	private static final String CONTENT_LENGTH = "Content-Length";

	private static final String TRANSFER_ENCODING = "Transfer-Encoding";

	private static String DEFAULT_BASE_UAA_URL = "https://uaa.cloudfoundry.com";

	private static final String COOKIE = "Cookie";

	private static final String SET_COOKIE = "Set-Cookie";

	private static final String COOKIE_MODEL = "cookie";

    @Autowired
	private RestOperations authorizationTemplate = null;

    @Value("${uaaHost}")
    private String baseUrl = "http://uaa.cloudfoundry.com";

	private String uaaHost;
    
    @Value("${tokenEndpoint:${uaaHost}}")
    public String tokenEndpoint = uaaHost;
    
    @Value("${entityID}")
    public String entityID = "";
	
	private static final String HOST = "Host";
	
	private Properties gitProperties = new Properties();
	
	private Map<String, String> links = new HashMap<String, String>();

	public CloudfoundryServiceProviderController() {
		setUaaBaseUrl(DEFAULT_BASE_UAA_URL);
		try {
			gitProperties = PropertiesLoaderUtils.loadAllProperties("git.properties");
		}
		catch (IOException e) {
			// Ignore
		}
	}

	/**
	 * @param baseUrl the base uaa url
	 */
	public void setUaaBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
		try {
			this.uaaHost = new URI(baseUrl).getHost();
		}
		catch (URISyntaxException e) {
			throw new IllegalArgumentException("Could not extract host from URI: " + baseUrl);
		}
	}

	public void setAuthorizationTemplate(RestOperations authorizationTemplate) {
		this.authorizationTemplate = authorizationTemplate;
		if (authorizationTemplate instanceof RestTemplate) {
			((RestTemplate) authorizationTemplate).setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
				@Override
				protected void postProcessHttpRequest(HttpUriRequest request) {
					super.postProcessHttpRequest(request);
					request.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
				}
			});
		}
		((RestTemplate) authorizationTemplate).setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
			@Override
			public HttpClient getHttpClient() {
				HttpClient client = super.getHttpClient();
				client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
				return client;
			}
		});
		((RestTemplate) authorizationTemplate).setErrorHandler(new DefaultResponseErrorHandler() {
			public boolean hasError(ClientHttpResponse response) throws IOException {
				HttpStatus statusCode = response.getStatusCode();
				return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
			}
		});
	}

	@RequestMapping(value = { "/info", "/login" }, method = RequestMethod.GET)
	public String loginInfo(HttpServletRequest request, @RequestHeader HttpHeaders headers, Model model,
			Principal principal) throws Exception {
		String path = extractPath(request);
		model.addAttribute("entityID", entityID);
		model.addAllAttributes(getLoginInfo(baseUrl + "/" + path, getRequestHeaders(headers)));
		model.addAllAttributes(getBuildInfo());
		model.addAttribute("links", getLinksInfo());
		if (principal == null) {
			return "login";
		}
		return "home";
	}

	private Map<String, ?> getLinksInfo() {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("uaa", baseUrl);
		model.put("login", baseUrl.replaceAll("uaa", "login"));
		model.putAll(links);
		return model;
	}

	private Map<String, ?> getBuildInfo() {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("commit_id", gitProperties.getProperty("git.commit.id.abbrev", "UNKNOWN"));
		model.put(
				"timestamp",
				gitProperties.getProperty("git.commit.time",
						new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())));
		return model;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, Object> getLoginInfo(String baseUrl, HttpHeaders headers) {
		ResponseEntity<Map> response = null;
		try {
			ResponseEntity<Map> entity = authorizationTemplate.exchange(baseUrl, HttpMethod.GET, new HttpEntity<Void>(null,
					headers), Map.class);
			response = entity;
		}
		catch (Exception e) {
			// use defaults
		}
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		if (response != null && response.getStatusCode() == HttpStatus.OK) {
			body.putAll((Map<String, Object>) response.getBody());
		}
		else {
			log.error("Cannot determine login info from remote server; using defaults");
			Map<String, String[]> prompts = new LinkedHashMap<String, String[]>();
			prompts.put("username", new String[] { "text", "Email" });
			prompts.put("password", new String[] { "password", "Password" });
			body.put("prompts", prompts);
		}
		return body;
	}
	
	@RequestMapping(value = { "/", "/home" }, method = RequestMethod.GET)
	public String home(Model model, Principal principal) throws Exception {
		return "home";
	}

	@RequestMapping(value = "/oauth/authorize", params = "response_type", method = RequestMethod.GET)
	public ModelAndView startAuthorization(HttpServletRequest request, @RequestParam Map<String, String> parameters,
			Map<String, Object> model, @RequestHeader HttpHeaders headers, Principal principal) {

		Authentication token = (Authentication) principal;
		String username = null;
		if(token.getPrincipal() instanceof NameIDImpl)
			username = ((NameIDImpl)token.getPrincipal()).getValue();
		else
			username = (String) token.getPrincipal();
				
		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.setAll(parameters);
		if (principal != null) {
			map.set("login", "{\"username\":\"" + username + "\"}");
		}

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.putAll(getRequestHeaders(headers));
		requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		requestHeaders.remove(COOKIE);
		requestHeaders.remove(COOKIE.toLowerCase());

		String path = extractPath(request);

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response;
		
		try {
			response = authorizationTemplate.exchange(baseUrl + "/" + path, HttpMethod.POST,
					new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders), Map.class);
		}
		catch (RuntimeException e) {
			if (authorizationTemplate instanceof OAuth2RestTemplate) {
				((OAuth2RestTemplate) authorizationTemplate).getOAuth2ClientContext().setAccessToken(null);
				response = authorizationTemplate.exchange(baseUrl + "/" + path, HttpMethod.POST,
						new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders), Map.class);
			}
			else {
				throw e;
			}
		}

		saveCookie(response.getHeaders(), model);

		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) response.getBody();
		if (body != null) {
			// User approval is required
			log.debug("Response: " + body);
			model.putAll(body);
			if (!body.containsKey("options")) {
				throw new OAuth2Exception("No options returned from UAA for user approval");
			}
			log.info("Approval required in /oauth/authorize for: " + principal.getName());
			return new ModelAndView("access_confirmation", model);
		}

		String location = response.getHeaders().getFirst("Location");
		if (location != null) {
			log.info("Redirect in /oauth/authorize for: " + principal.getName());
			return new ModelAndView(new RedirectView(location, false, true, false));
		}

		throw new IllegalStateException("Neither a redirect nor a user approval");
    }

	@RequestMapping(value = "/oauth/authorize", method = RequestMethod.POST, params = "user_oauth_approval")
	@ResponseBody
	public ResponseEntity<byte[]> approveOrDeny(HttpServletRequest request, HttpEntity<byte[]> entity,
			Map<String, Object> model, SessionStatus sessionStatus) throws Exception {
		sessionStatus.setComplete();
		return passthru(request, entity, model);
	}

	@RequestMapping(value = "/oauth/authorize", method = RequestMethod.POST, params = "credentials")
	@ResponseBody
	public ResponseEntity<byte[]> implicitOld(HttpServletRequest request, HttpEntity<byte[]> entity,
			Map<String, Object> model) throws Exception {
		log.info("Direct authentication request with JSON credentials at /oauth/authorize");
		return passthru(request, entity, model);
	}

	@RequestMapping(value = "/oauth/authorize", method = RequestMethod.POST, params = "source=credentials")
	@ResponseBody
	public ResponseEntity<byte[]> implicit(HttpServletRequest request, HttpEntity<byte[]> entity,
			Map<String, Object> model) throws Exception {
		log.info("Direct authentication request at /oauth/authorize for " + request.getParameter("username"));
		return passthru(request, entity, model);
	}
	
	// We do not map /oauth/confirm_access because we want to remove the remote session cookie in approveOrDeny
	@RequestMapping(value = "/oauth/**")
	@ResponseBody
	public void invalid(HttpServletRequest request) throws Exception {
		throw new OAuth2Exception("no matching handler for request: " + request.getServletPath());
	}
	
//	@RequestMapping(value = { "/error" }, method = RequestMethod.GET)
//	public String error(HttpServletRequest request, @RequestHeader HttpHeaders headers, Model model,
//			Principal principal) throws Exception {
//		return "error";
//	}
	
	public static Map<String, ?> getMapFromProperties(Properties properties, String prefix) {
		Map<String, Object> result = new HashMap<String, Object>();
		for (String key : properties.stringPropertyNames()) {
			if (key.startsWith(prefix)) {
				String name = key.substring(prefix.length());
				result.put(name, properties.getProperty(key));
			}
		}
		return result;
	}
	
	private void saveCookie(HttpHeaders headers, Map<String, Object> model) {
		if (!headers.containsKey(SET_COOKIE)) {
			return;
		}
		StringBuilder cookie = new StringBuilder();
		// Save back end cookie for later
		for (String value : headers.get(SET_COOKIE)) {
			if (value.contains(";")) {
				value = value.substring(0, value.indexOf(";"));
			}
			if (cookie.length()>0) {
				cookie.append(";");
			}
			cookie.append(value);
		}
		log.debug("Saved back end cookies: " + cookie);
		model.put(COOKIE_MODEL, cookie.toString());
	}
	
	@ExceptionHandler(OAuth2Exception.class)
	public ModelAndView handleOAuth2Exception(OAuth2Exception e, ServletWebRequest webRequest) throws Exception {
		log.info("OAuth2 error" + e.getSummary());
		webRequest.getResponse().setStatus(e.getHttpErrorCode());
		return new ModelAndView("forward:/", Collections.singletonMap("error", e));
	}

	@ExceptionHandler(ResourceAccessException.class)
	public ModelAndView handleRestClientException(ResourceAccessException e) throws Exception {
		log.info("Rest client error: " + e.getMessage());
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		Map<String, Object> model = new HashMap<String, Object>();
		model.putAll(getLoginInfo(baseUrl + "/login", getRequestHeaders(headers)));
		model.putAll(getBuildInfo());
		Map<String, String> error = new LinkedHashMap<String, String>();
		error.put("error", "rest_client_error");
		error.put("error_description", e.getMessage());
		model.put("error", error);
		return new ModelAndView("login", model);
	}

	private ResponseEntity<byte[]> passthru(HttpServletRequest request, HttpEntity<byte[]> entity,
			Map<String, Object> model) throws Exception {

		String path = extractPath(request);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.putAll(getRequestHeaders(entity.getHeaders()));
		// Get back end cookie if saved in session
		String cookie = (String) model.get("cookie");
		if (cookie != null) {
			log.debug("Found back end cookie: " + cookie);
			requestHeaders.set("Cookie", cookie);
		}

		ResponseEntity<byte[]> response = authorizationTemplate.exchange(uaaHost + "/" + path, HttpMethod.POST,
				new HttpEntity<byte[]>(entity.getBody(), requestHeaders), byte[].class);
		HttpHeaders outgoingHeaders = getResponseHeaders(response.getHeaders());
		return new ResponseEntity<byte[]>(response.getBody(), outgoingHeaders, response.getStatusCode());

	}
	
	private HttpHeaders getResponseHeaders(HttpHeaders headers) {
		// Some of the headers coming back are poisonous apparently (content-length?)...
		HttpHeaders outgoingHeaders = new HttpHeaders();
		outgoingHeaders.putAll(headers);
		if (headers.getContentLength() >= 0) {
			outgoingHeaders.remove(CONTENT_LENGTH);
			outgoingHeaders.remove(CONTENT_LENGTH.toLowerCase());
		}
		if (headers.containsKey(TRANSFER_ENCODING)) {
			outgoingHeaders.remove(TRANSFER_ENCODING);
			outgoingHeaders.remove(TRANSFER_ENCODING.toLowerCase());
		}
		return outgoingHeaders;
	}
	
	private String extractPath(HttpServletRequest request) {
		String query = request.getQueryString();
		try {
			query = query == null ? "" : "?" + URLDecoder.decode(query, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("Cannot decode query string: " + query);
		}
		String path = request.getRequestURI() + query;
		String context = request.getContextPath();
		path = path.substring(context.length());
		if (path.startsWith("/")) {
			// In the root context we have to remove this as well
			path = path.substring(1);
		}
		log.debug("Path: " + path);
		return path;
	}

	private HttpHeaders getRequestHeaders(HttpHeaders headers) {
		// Some of the headers coming back are poisonous apparently (content-length?)...
		HttpHeaders outgoingHeaders = new HttpHeaders();
		outgoingHeaders.putAll(headers);
		outgoingHeaders.remove(HOST);
		outgoingHeaders.remove(HOST.toLowerCase());
		outgoingHeaders.set(HOST, uaaHost);
		log.debug("Outgoing headers: " + outgoingHeaders);
		return outgoingHeaders;
	}


	public RestOperations getAuthorizationTemplate() {
		return authorizationTemplate;
	}

	public void setTokenEndpoint(String tokenEndpoint) {
		this.tokenEndpoint = tokenEndpoint;
	}

	public String getEntityID() {
		return entityID;
	}

	public void setEntityID(String entityID) {
		this.entityID = entityID;
	}
}
