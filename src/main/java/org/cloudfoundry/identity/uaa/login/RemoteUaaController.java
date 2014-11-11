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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.cloudfoundry.identity.uaa.authentication.AuthzAuthenticationRequest;
import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthenticationDetails;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.authentication.login.Prompt;
import org.cloudfoundry.identity.uaa.client.SocialClientUserDetails;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.cloudfoundry.identity.uaa.oauth.UaaOauth2ErrorHandler;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Controller that manages OAuth authorization via a remote UAA service. Use
 * this in conjunction with the authentication
 * mechanism of your choice (Google OpenID etc.) to serve OAuth2 tokens to
 * clients registered in the remote
 * server.
 * 
 * @author Dave Syer
 * 
 */
@Controller
@SessionAttributes(value = "cookie")
public class RemoteUaaController extends AbstractControllerInfo {

    private static final Log logger = LogFactory.getLog(RemoteUaaController.class);

    private static final String CONTENT_LENGTH = "Content-Length";

    protected static final String CONTENT_TYPE = "Content-Type";

    protected static final String ACCEPT = "Accept";

    protected static final String AUTHORIZATION = "Authorization";

    private static final String TRANSFER_ENCODING = "Transfer-Encoding";

    protected static final String COOKIE = "Cookie";

    private static final String SET_COOKIE = "Set-Cookie";

    private static final String COOKIE_MODEL = "cookie";

    private static final String USER_AGENT = "user-agent";

    protected final Environment environment;

    private RestOperations defaultTemplate = new RestTemplate();

    private RestOperations authorizationTemplate = new RestTemplate();

    private List<Prompt> prompts;

    private long codeExpirationMillis = 5 * 60 * 1000;

    private AuthenticationManager remoteAuthenticationManager;

    public long getCodeExpirationMillis() {
        return codeExpirationMillis;
    }

    public void setCodeExpirationMillis(long codeExpirationMillis) {
        this.codeExpirationMillis = codeExpirationMillis;
    }

    public AuthenticationManager getRemoteAuthenticationManager() {
        return remoteAuthenticationManager;
    }

    public void setRemoteAuthenticationManager(AuthenticationManager remoteAuthenticationManager) {
        this.remoteAuthenticationManager = remoteAuthenticationManager;
    }

    /**
     * Prompts to use if authenticating locally. Set this if you want to
     * override the default behaviour of asking the
     * remote UAA for its prompts.
     * 
     * @param prompts the prompts to set
     */
    public void setPrompts(List<Prompt> prompts) {
        this.prompts = prompts;
    }

    /**
     * @param authorizationTemplate the authorizationTemplate to set
     */
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
    }

    public RemoteUaaController(Environment environment, RestTemplate restTemplate) {

        // The default java.net client doesn't allow you to handle 4xx responses
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory() {
            @Override
            public HttpClient getHttpClient() {
                return HttpClientBuilder.create().useSystemProperties().disableCookieManagement().build();
            }
        });
        if (restTemplate instanceof OAuth2RestTemplate) {
            OAuth2RestTemplate oAuth2RestTemplate = (OAuth2RestTemplate)restTemplate;
            oAuth2RestTemplate.setErrorHandler(new UaaOauth2ErrorHandler(oAuth2RestTemplate.getResource(), HttpStatus.Series.SERVER_ERROR));
        } else {
            restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
                @Override
                public boolean hasError(ClientHttpResponse response) throws IOException {
                    HttpStatus statusCode = response.getStatusCode();
                    return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
                }
            });
        }
        this.environment = environment;
        defaultTemplate = restTemplate;
        initProperties();
    }

    @RequestMapping(value = { "/login", "/info" }, method = RequestMethod.GET)
    public String prompts(HttpServletRequest request, @RequestHeader HttpHeaders headers, Map<String, Object> model,
                    Principal principal) throws Exception {
        String path = extractPath(request);
        model.putAll(getLoginInfo(getUaaBaseUrl() + "/" + path, getRequestHeaders(headers)));
        model.put("links", getLinksInfo());
        setCommitInfo(model);
        if (principal == null) {
            boolean selfServiceLinksEnabled = !"false".equalsIgnoreCase(environment.getProperty("login.selfServiceLinksEnabled"));
            if (selfServiceLinksEnabled) {
                String customSignupLink = environment.getProperty("links.signup");
                String customPasswordLink = environment.getProperty("links.passwd");
                if (StringUtils.hasText(customSignupLink)) {
                    model.put("createAccountLink", customSignupLink);
                } else {
                    model.put("createAccountLink", "/create_account");
                }
                if (StringUtils.hasText(customPasswordLink)) {
                    model.put("forgotPasswordLink", customPasswordLink);
                } else {
                    model.put("forgotPasswordLink", "/forgot_password");
                }
            }
            return "login";
        }
        return "home";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Map<String, Object> getLoginInfo(String baseUrl, HttpHeaders headers) {

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        // If prompts are configured explicitly use them
        if (prompts != null) {
            Map<String, String[]> map = new LinkedHashMap<String, String[]>();
            for (Prompt prompt : prompts) {
                map.put(prompt.getName(), prompt.getDetails());
            }
            body.put("prompts", map);
            return body;
        }

        // Otherwise fetch prompts from remote UAA
        ResponseEntity<Map> response = null;
        try {
            ResponseEntity<Map> entity = defaultTemplate.exchange(baseUrl, HttpMethod.GET, new HttpEntity<Void>(null,
                            headers), Map.class);
            response = entity;
        } catch (Exception e) {
            // use defaults
        }
        if (response != null && response.getStatusCode() == HttpStatus.OK) {
            body.putAll(response.getBody());
        }
        else {
            logger.error("Cannot determine login info from remote server; using defaults");
            Map<String, String[]> prompts = new LinkedHashMap<String, String[]>();
            prompts.put("username", new String[] { "text", "Email" });
            prompts.put("password", new String[] { "password", "Password" });
            body.put("prompts", prompts);
        }

        return body;
    }

    @RequestMapping(value = "/oauth/authorize", params = "response_type")
    public ModelAndView startAuthorization(HttpServletRequest request, @RequestParam Map<String, String> parameters,
                    Map<String, Object> model, @RequestHeader HttpHeaders headers, Principal principal)
                    throws Exception {

        String path = extractPath(request);

        MultiValueMap<String, String> map = new LinkedMaskingMultiValueMap<String, String>();
        map.setAll(parameters);

        String redirectUri = parameters.get("redirect-uri");
        if (redirectUri != null && !redirectUri.matches("(http:|https:)?//.*")) {
            redirectUri = "http://" + redirectUri;
            map.set("redirect-uri", redirectUri);
        }

        if (principal != null) {
            map.set("source", "login");
            map.setAll(getLoginCredentials(principal));
            map.remove("credentials"); // legacy cf might break otherwise
            map.remove("password"); // request for token will not use password
        }
        else {
            throw new BadCredentialsException("No principal found in authorize endpoint");
        }

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.putAll(getRequestHeaders(headers));
        requestHeaders.remove(AUTHORIZATION.toLowerCase());
        requestHeaders.remove(USER_AGENT);
        requestHeaders.remove(ACCEPT.toLowerCase());
        requestHeaders.remove(CONTENT_TYPE.toLowerCase());
        requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        requestHeaders.remove(COOKIE);
        requestHeaders.remove(COOKIE.toLowerCase());

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> response;

        response = authorizationTemplate.exchange(getUaaBaseUrl() + "/" + path, HttpMethod.POST,
                        new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders), Map.class);

        saveCookie(response.getHeaders(), model);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = response.getBody();
        if (body != null) {
            // User approval is required
            logger.debug("Response: " + body);
            model.putAll(body);
            model.put("links", getLinksInfo());
            if (!body.containsKey("options")) {
                String errorMsg = "No options returned from UAA for user approval";
                if (body.containsKey("error")) {
                    throw OAuth2Exception.create((String)body.get("error"), (String)(body.containsKey("error_description")?body.get("error_description"):errorMsg));
                } else {
                    throw new OAuth2Exception(errorMsg);
                }
            }
            logger.info("Approval required in /oauth/authorize for: " + principal.getName());
            return new ModelAndView("access_confirmation", model);
        }

        String location = response.getHeaders().getFirst("Location");
        if (location != null) {
            logger.info("Redirect in /oauth/authorize for: " + principal.getName());
            // Don't expose model attributes (cookie) in redirect
            return new ModelAndView(new RedirectView(location, false, true, false));
        }

        throw new IllegalStateException("Neither a redirect nor a user approval");

    }

    @RequestMapping(value = "/oauth/authorize", method = RequestMethod.POST, params = "user_oauth_approval")
    @ResponseBody
    public ResponseEntity<byte[]> approveOrDeny(HttpServletRequest request, HttpEntity<byte[]> entity,
                    Map<String, Object> model, SessionStatus sessionStatus) throws Exception {
        sessionStatus.setComplete();
        return passthru(request, entity, model, false);
    }

    @RequestMapping(value = { "/oauth/token" } , params = "grant_type=password")
    @ResponseBody
    public ResponseEntity<byte[]> passwordGrant(HttpServletRequest request,
                                                @RequestHeader("Authorization") String authorization,
                                                @RequestHeader HttpHeaders headers,
                                                @RequestBody MultiValueMap<String, String> originalBody,
                                                Map<String, Object> model,
                                                Principal principal) throws Exception {
        logger.info("Passing through password grant token request for " + request.getServletPath());

        Set<String> maskedAttribute = new HashSet<>();
        maskedAttribute.add("password");
        maskedAttribute.add("client_secret");
        LinkedMaskingMultiValueMap<String,String> body = new LinkedMaskingMultiValueMap<>(maskedAttribute);
        for (Map.Entry<String, List<String>> entry : originalBody.entrySet()) {
            body.put(entry.getKey(), entry.getValue());
        }

        body.setAll(getLoginCredentials(principal));
        //for grant_type=password, we want to do user authentication
        //in the login server rather than in UAA
        String[] basic = extractAndDecodeHeader(authorization);
        //create a modifiable list
        headers = getRequestHeaders(headers);
        headers.remove(AUTHORIZATION);
        headers.remove(AUTHORIZATION.toLowerCase());
        body.remove("client_id");
        body.add("client_id", basic[0]);
        body.add("client_secret", basic[1]);
        body.add("source", "login");

        //remove multiple values as the UAA can't handle it
        body.remove("grant_type");
        if (!extractPath(request).contains("grant_type")) {
            body.add("grant_type", "password");
        }

        HttpEntity entity = new HttpEntity(body, headers);
        return passthru(request, entity, model, true);
    }

    /**
     * Decodes the header into a username and password.
     *
     * @throws BadCredentialsException if the Basic header is not present or is not valid Base64
     */
    private String[] extractAndDecodeHeader(String header) throws IOException {

        byte[] base64Token = header.substring(6).getBytes("UTF-8");
        byte[] decoded;
        try {
            decoded = org.springframework.security.crypto.codec.Base64.decode(base64Token);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Failed to decode basic authentication token");
        }

        String token = new String(decoded, "UTF-8");

        int delim = token.indexOf(":");

        if (delim == -1) {
            throw new BadCredentialsException("Invalid basic authentication token");
        }
        return new String[] {token.substring(0, delim), token.substring(delim + 1)};
    }

    @RequestMapping(value = { "/oauth/error", "oauth/token" })
    @ResponseBody
    public ResponseEntity<byte[]> sundry(HttpServletRequest request, HttpEntity<byte[]> entity,
                    Map<String, Object> model) throws Exception {
        logger.info("Pass through request for " + request.getServletPath());
        return passthru(request, entity, model, false);
    }

    // We do not map /oauth/confirm_access because we want to remove the remote
    // session cookie in approveOrDeny
    @RequestMapping(value = "/oauth/**")
    @ResponseBody
    public void invalid(HttpServletRequest request) throws Exception {
        throw new OAuth2Exception("no matching handler for request: " + request.getServletPath());
    }

    @RequestMapping(value = "/autologin", method = RequestMethod.POST)
    @ResponseBody
    public AutologinResponse generateAutologinCode(@RequestBody AutologinRequest request,
                    @RequestHeader(value = "Authorization", required = false) String auth) throws Exception {
        if (auth == null || (!auth.startsWith("Basic"))) {
            throw new BadCredentialsException("No basic authorization client information in request");
        }

        String username = request.getUsername();
        if (username == null) {
            throw new BadCredentialsException("No username in request");
        }
        Authentication remoteAuthentication = null;
        if (remoteAuthenticationManager != null) {
            String password = request.getPassword();
            if (!StringUtils.hasText(password)) {
                throw new BadCredentialsException("No password in request");
            }
            remoteAuthentication = remoteAuthenticationManager.authenticate(new AuthzAuthenticationRequest(username, password, null));
        }

        String base64Credentials = auth.substring("Basic".length()).trim();
        String credentials = new String(new Base64().decode(base64Credentials.getBytes()), Charset.forName("UTF-8"));
        // credentials = username:password
        final String[] values = credentials.split(":", 2);
        if (values == null || values.length == 0) {
            throw new BadCredentialsException("Invalid authorization header.");
        }
        String clientId = values[0];
        logger.debug("Autologin authentication request for user:" + username + "; client:" + clientId);
        SocialClientUserDetails user = new SocialClientUserDetails(username, UaaAuthority.USER_AUTHORITIES);
        Map<String,String> details = new HashMap<>();
        details.put("client_id", clientId);
        user.setDetails(details);
        if (remoteAuthentication!=null && remoteAuthentication.getPrincipal() instanceof UaaPrincipal) {
            UaaPrincipal p = (UaaPrincipal)remoteAuthentication.getPrincipal();
            if (p!=null) {
                details.put(Origin.ORIGIN, p.getOrigin());
                details.put("user_id",p.getId());
            }
        }

        ResponseEntity<ExpiringCode> response = doGenerateCode(user);
        return new AutologinResponse(response.getBody().getCode());
    }

    protected ResponseEntity<ExpiringCode> doGenerateCode(Object o) throws IOException {
        ExpiringCode ec = new ExpiringCode(null,
                        new Timestamp(System.currentTimeMillis() + (getCodeExpirationMillis())),
                        new ObjectMapper().writeValueAsString(o));

        // ec = generateCode
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        requestHeaders.add(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<ExpiringCode> requestEntity = new HttpEntity<ExpiringCode>(ec, requestHeaders);

        ResponseEntity<ExpiringCode> response = authorizationTemplate.exchange(getUaaBaseUrl() + "/Codes",
                        HttpMethod.POST,
                        requestEntity, ExpiringCode.class);

        if (response.getStatusCode() != HttpStatus.CREATED) {
            logger.warn("Request failed: " + requestEntity);
            // TODO throw exception with the correct error
            throw new RuntimeException(String.valueOf(response.getStatusCode()));
        }

        return response;
    }

    @ExceptionHandler(OAuth2Exception.class)
    public ModelAndView handleOAuth2Exception(OAuth2Exception e, ServletWebRequest webRequest) throws Exception {
        logger.info(e.getSummary());
        int errorCode = e.getHttpErrorCode();
        if (errorCode!=401 && "Bad credentials".equals(e.getMessage())) {
            //https://github.com/spring-projects/spring-security-oauth/issues/191
            errorCode = 401;
        }
        webRequest.getResponse().setStatus(errorCode);
        return new ModelAndView("forward:/home", Collections.singletonMap("error", e.getSummary()));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ModelAndView handleRestClientException(ResourceAccessException e) throws Exception {
        logger.info("Rest client error: " + e.getMessage());
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        Map<String, Object> model = new HashMap<String, Object>();
        model.putAll(getLoginInfo(getUaaBaseUrl() + "/login", getRequestHeaders(headers)));
        Map<String, String> error = new LinkedHashMap<String, String>();
        error.put("error", "rest_client_error");
        error.put("error_description", e.getMessage());
        model.put("error", error);
        return new ModelAndView("login", model);
    }

    protected void saveCookie(HttpHeaders headers, Map<String, Object> model) {
        if (!headers.containsKey(SET_COOKIE)) {
            return;
        }
        StringBuilder cookie = new StringBuilder();
        // Save back end cookie for later
        for (String value : headers.get(SET_COOKIE)) {
            if (value.contains(";")) {
                value = value.substring(0, value.indexOf(";"));
            }
            if (cookie.length() > 0) {
                cookie.append(";");
            }
            cookie.append(value);
        }
        logger.debug("Saved back end cookies: " + cookie);
        model.put(COOKIE_MODEL, cookie.toString());
    }

    protected Map<String, String> getLoginCredentials(Principal principal) {
        Map<String, String> login = new LinkedHashMap<String, String>();
        appendField(login, "username", principal.getName());
        if (principal instanceof UaaPrincipal) {
            appendField(login, "user_id", ((UaaPrincipal)principal).getId());
            appendField(login, Origin.ORIGIN, ((UaaPrincipal)principal).getOrigin());
            appendField(login, UaaAuthenticationDetails.ADD_NEW, "false");
        } else if (principal instanceof Authentication) {
            Object details = ((Authentication) principal).getPrincipal();
            if (details instanceof UaaPrincipal) {
                appendField(login, "user_id", ((UaaPrincipal)details).getId());
                appendField(login, Origin.ORIGIN, ((UaaPrincipal)details).getOrigin());
                appendField(login, UaaAuthenticationDetails.ADD_NEW, "false");
            } else if (details instanceof SocialClientUserDetails) {
                SocialClientUserDetails user = (SocialClientUserDetails) details;
                appendField(login, "name", user.getName());
                appendField(login, "external_id", user.getExternalId());
                appendField(login, "email", user.getEmail());
                appendField(login, Origin.ORIGIN, user.getSource());
                appendField(login, UaaAuthenticationDetails.ADD_NEW, "true");
            }
        }
        return login;
    }

    protected void appendField(Map<String, String> login, String key, Object value) {
        if (value != null) {
            login.put(key, value.toString());
        }
    }

    protected ResponseEntity<byte[]> passthru(HttpServletRequest request, HttpEntity entity,
                    Map<String, Object> model, boolean loginClientRequired) throws Exception {

        String path = extractPath(request);

        RestOperations template = loginClientRequired?getAuthorizationTemplate():getDefaultTemplate();
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.putAll(getRequestHeaders(entity.getHeaders()));
        requestHeaders.remove(COOKIE);
        requestHeaders.remove(COOKIE.toLowerCase());
        // Get back end cookie if saved in session
        String cookie = (String) model.get(COOKIE_MODEL);
        if (cookie != null) {
            logger.debug("Found back end cookies: " + cookie);
            for (String value : cookie.split(";")) {
                requestHeaders.add(COOKIE, value);
            }
        }

        ResponseEntity<byte[]> response = template.exchange(
            getUaaBaseUrl() + "/" + path,
            HttpMethod.valueOf(request.getMethod()),
            new HttpEntity(entity.getBody(),requestHeaders),
            byte[].class);
        HttpHeaders outgoingHeaders = getResponseHeaders(response.getHeaders());
        return new ResponseEntity<byte[]>(response.getBody(), outgoingHeaders, response.getStatusCode());

    }

    protected HttpHeaders getResponseHeaders(HttpHeaders headers) {
        // Some of the headers coming back are poisonous apparently
        // (content-length?)...
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

    public RestOperations getAuthorizationTemplate() {
        return authorizationTemplate;
    }

    public RestOperations getDefaultTemplate() {
        return defaultTemplate;
    }
}
