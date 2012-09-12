package org.cloudfoundry.identity.uaa.login;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.social.SocialClientUserDetails;
import org.cloudfoundry.identity.uaa.util.UaaStringUtils;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
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

/**
 * Controller that manages OAuth authorization via a remote UAA service. Use this in conjunction with the authentication
 * mechanism of your choice (LDAP, Google OpenID etc.) to serve OAuth2 tokens to clients registered in the remote
 * server.
 * 
 * @author Dave Syer
 * 
 */
@Controller
@SessionAttributes(value = "cookie")
public class RemoteUaaController {

	private static final Log logger = LogFactory.getLog(RemoteUaaController.class);

	private static final String CONTENT_LENGTH = "Content-Length";

	private static final String TRANSFER_ENCODING = "Transfer-Encoding";

	private static final String HOST = "Host";

	private static String DEFAULT_BASE_UAA_URL = "https://uaa.cloudfoundry.com";

	private Properties gitProperties = new Properties();

	private Properties buildProperties = new Properties();

	private RestTemplate defaultTemplate = new RestTemplate();

	private RestOperations authorizationTemplate = new RestTemplate();

	private String baseUrl;

	private String uaaHost;

	/**
	 * @param authorizationTemplate the authorizationTemplate to set
	 */
	public void setAuthorizationTemplate(RestOperations authorizationTemplate) {
		this.authorizationTemplate = authorizationTemplate;
	}

	public RemoteUaaController() {
		// The default java.net client doesn't allow you to handle 4xx responses
		defaultTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
		defaultTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			public boolean hasError(ClientHttpResponse response) throws IOException {
				HttpStatus statusCode = response.getStatusCode();
				return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
			}
		});
		setUaaBaseUrl(DEFAULT_BASE_UAA_URL);
		try {
			gitProperties = PropertiesLoaderUtils.loadAllProperties("git.properties");
		}
		catch (IOException e) {
			// Ignore
		}
		try {
			buildProperties = PropertiesLoaderUtils.loadAllProperties("build.properties");
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

	@RequestMapping(value = { "/login", "/login_info" }, method = RequestMethod.GET)
	public String prompts(HttpServletRequest request, @RequestHeader HttpHeaders headers, Model model,
			Principal principal) throws Exception {
		String path = extractPath(request);
		model.addAllAttributes(getLoginInfo(baseUrl + "/" + path, getRequestHeaders(headers)));
		model.addAllAttributes(getBuildInfo());
		if (principal == null) {
			return "login";
		}
		return "home";
	}

	private Map<String, ?> getBuildInfo() {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("commit_id", gitProperties.getProperty("git.commit.id.abbrev", "UNKNOWN"));
		model.put(
				"timestamp",
				gitProperties.getProperty("git.commit.time",
						new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date())));
		model.put("app", UaaStringUtils.getMapFromProperties(buildProperties, "build."));
		model.put("uaa", baseUrl);
		return model;
	}

	private Map<String, Object> getLoginInfo(String baseUrl, HttpHeaders headers) {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response = defaultTemplate.exchange(baseUrl, HttpMethod.GET, new HttpEntity<Void>(null,
				headers), Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) response.getBody();
		return body;
	}

	@RequestMapping(value = "/oauth/authorize", params = "response_type", method = RequestMethod.GET)
	public ModelAndView startAuthorization(HttpServletRequest request, @RequestParam Map<String, String> parameters,
			Map<String, Object> model, @RequestHeader HttpHeaders headers, Principal principal) throws Exception {

		String path = extractPath(request);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		map.setAll(parameters);
		if (principal != null) {
			map.set("login", getLoginCredentials(principal));
		} else {
			throw new BadCredentialsException("No principal found in authorize endpoint");
		}

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.putAll(getRequestHeaders(headers));
		requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		requestHeaders.remove("Cookie");

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response;

		try {
			response = authorizationTemplate.exchange(baseUrl + "/" + path, HttpMethod.POST,
					new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders), Map.class);
		}
		catch (RuntimeException e) {
			// Defensive workaround for SECOAUTH-335
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
			logger.debug("Response: " + body);
			model.putAll(body);
			if (!body.containsKey("options")) {
				throw new OAuth2Exception("No options returned from UAA for user approval");
			}
			logger.info("Approval required in /oauth/authorize for: " + principal.getName());
			return new ModelAndView("access_confirmation", model);
		}

		String location = response.getHeaders().getFirst("Location");
		if (location != null) {
			logger.info("Redirect in /oauth/authorize for: " + principal.getName());
			return new ModelAndView(new RedirectView(location));
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
		logger.info("Direct authentication request with JSON credentials at /oauth/authorize");
		return passthru(request, entity, model);
	}

	@RequestMapping(value = "/oauth/authorize", method = RequestMethod.POST, params = "source=credentials")
	@ResponseBody
	public ResponseEntity<byte[]> implicit(HttpServletRequest request, HttpEntity<byte[]> entity,
			Map<String, Object> model) throws Exception {
		logger.info("Direct authentication request at /oauth/authorize for " + request.getParameter("username"));
		return passthru(request, entity, model);
	}

	@RequestMapping(value = { "/oauth/error", "oauth/token" })
	@ResponseBody
	public ResponseEntity<byte[]> sundry(HttpServletRequest request, HttpEntity<byte[]> entity,
			Map<String, Object> model) throws Exception {
		logger.info("Pass through request for " + request.getServletPath());
		return passthru(request, entity, model);
	}

	// We do not map /oauth/confirm_access because we want to remove the remote session cookie in approveOrDeny
	@RequestMapping(value = "/oauth/**")
	@ResponseBody
	public void invalid(HttpServletRequest request) throws Exception {
		throw new OAuth2Exception("no matching handler for request: " + request.getServletPath());
	}

	@ExceptionHandler(OAuth2Exception.class)
	public ModelAndView handleOAuth2Exception(OAuth2Exception e, ServletWebRequest webRequest) throws Exception {
		logger.info("OAuth2 error" + e.getSummary());
		webRequest.getResponse().setStatus(e.getHttpErrorCode());
		return new ModelAndView("forward:/home", Collections.singletonMap("error", e.getSummary()));
	}

	@ExceptionHandler(ResourceAccessException.class)
	public ModelAndView handleRestClientException(ResourceAccessException e) throws Exception {
		logger.info("Rest client error" + e.getMessage());
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		Map<String, Object> model = new HashMap<String, Object>();
		Map<String, String[]> prompts = new LinkedHashMap<String, String[]>();
		prompts.put("username", new String[] { "text", "Email" });
		prompts.put("password", new String[] { "password", "Password" });
		model.put("prompts", prompts);
		model.putAll(getBuildInfo());
		Map<String, String> error = new LinkedHashMap<String, String>();
		error.put("error", "rest_client_error");
		error.put("error_description", e.getMessage());
		model.put("error", error);
		return new ModelAndView("login", model);
	}

	private void saveCookie(HttpHeaders headers, Map<String, Object> model) {
		// Save back end cookie for later
		String cookie = headers.getFirst("Set-Cookie");
		if (cookie != null) {
			logger.debug("Saved back end cookie: " + cookie);
			model.put("cookie", cookie);
		}
	}

	private String getLoginCredentials(Principal principal) {
		StringBuilder login = new StringBuilder("{");
		appendField(login, "username", principal.getName());
		if (principal instanceof Authentication) {
			Object details = ((Authentication) principal).getPrincipal();
			if (details instanceof SocialClientUserDetails) {
				SocialClientUserDetails user = (SocialClientUserDetails) details;
				appendField(login, "source", user.getSource());
				appendField(login, "name", user.getName());
				appendField(login, "external_id", user.getExternalId());
				appendField(login, "email", user.getEmail());
			}
		}
		login.append("}");
		return login.toString();
	}

	private void appendField(StringBuilder login, String key, Object value) {
		if (value != null) {
			if (login.length() > 1) {
				login.append(",");
			}
			quote(login, key).append(":");
			if (value instanceof CharSequence) {
				quote(login, (CharSequence) value);
			}
			else {
				login.append(value);
			}
		}
	}

	private StringBuilder quote(StringBuilder login, CharSequence string) {
		login.append("\"").append(string).append("\"");
		return login;
	}

	private ResponseEntity<byte[]> passthru(HttpServletRequest request, HttpEntity<byte[]> entity,
			Map<String, Object> model) throws Exception {

		String path = extractPath(request);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.putAll(getRequestHeaders(entity.getHeaders()));
		// Get back end cookie if saved in session
		String cookie = (String) model.get("cookie");
		if (cookie != null) {
			logger.debug("Found back end cookie: " + cookie);
			requestHeaders.set("Cookie", cookie);
		}

		ResponseEntity<byte[]> response = defaultTemplate.exchange(baseUrl + "/" + path,
				HttpMethod.valueOf(request.getMethod()), new HttpEntity<byte[]>(entity.getBody(), requestHeaders),
				byte[].class);
		HttpHeaders outgoingHeaders = getResponseHeaders(response.getHeaders());
		return new ResponseEntity<byte[]>(response.getBody(), outgoingHeaders, response.getStatusCode());

	}

	private HttpHeaders getResponseHeaders(HttpHeaders headers) {
		// Some of the headers coming back are poisonous apparently (content-length?)...
		HttpHeaders outgoingHeaders = new HttpHeaders();
		outgoingHeaders.putAll(headers);
		if (headers.getContentLength() >= 0) {
			outgoingHeaders.remove(CONTENT_LENGTH);
		}
		if (headers.containsKey(TRANSFER_ENCODING)) {
			outgoingHeaders.remove(TRANSFER_ENCODING);
		}
		return outgoingHeaders;
	}

	private HttpHeaders getRequestHeaders(HttpHeaders headers) {
		// Some of the headers coming back are poisonous apparently (content-length?)...
		HttpHeaders outgoingHeaders = new HttpHeaders();
		outgoingHeaders.putAll(headers);
		outgoingHeaders.remove(HOST);
		outgoingHeaders.set(HOST, uaaHost);
		logger.debug("Outgoing headers: " + outgoingHeaders);
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
		logger.debug("Path: " + path);
		return path;
	}

}
