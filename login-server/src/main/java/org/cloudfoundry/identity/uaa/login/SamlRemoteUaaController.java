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

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.client.SocialClientUserDetails;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.BaseClientDetails;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

public class SamlRemoteUaaController extends RemoteUaaController {

    private static final Log logger = LogFactory.getLog(SamlRemoteUaaController.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${login.entityID}")
    public String entityID = "";

    public SamlRemoteUaaController(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    @RequestMapping(value = { "/info", "/login" }, method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE)
    public String prompts(HttpServletRequest request, @RequestHeader HttpHeaders headers, Map<String, Object> model,
                    Principal principal) throws Exception {
        // Entity ID to start the discovery
        model.put("entityID", entityID);
        model.put("saml", Boolean.TRUE);
        return super.prompts(request, headers, model, principal);
    }

    @RequestMapping(value = { "/info", "/login" }, method = RequestMethod.GET, produces = TEXT_HTML_VALUE)
    public String samlUiPrompts(HttpServletRequest request, @RequestHeader HttpHeaders headers, Map<String, Object> model,
                          Principal principal) throws Exception {

        String logicalViewName = prompts(request, headers, model, principal);

        Map<String,Object> prompts = new LinkedHashMap<String, Object>((Map<String, Object>) model.get("prompts"));
        prompts.remove("passcode");
        model.put("prompts", prompts);

        return logicalViewName;
    }

    @Override
    protected Map<String, String> getLoginCredentials(Principal principal) {
        Map<String, String> login = new LinkedHashMap<String, String>();
        Collection<? extends GrantedAuthority> authorities = null;

        if (principal instanceof ExpiringUsernameAuthenticationToken) {
            appendField(login, "username",
                            ((SamlUserDetails) (((ExpiringUsernameAuthenticationToken) principal).getPrincipal()))
                                            .getUsername());

            authorities = ((SamlUserDetails) (((ExpiringUsernameAuthenticationToken) principal).getPrincipal()))
                            .getAuthorities();
        }
        else {
            appendField(login, "username", principal.getName());
        }

        if (principal instanceof Authentication) {
            Object details = ((Authentication) principal).getPrincipal();
            if (details instanceof SocialClientUserDetails) {
                SocialClientUserDetails user = (SocialClientUserDetails) details;
                appendField(login, "name", user.getName());
                appendField(login, "external_id", user.getExternalId());
                appendField(login, "email", user.getEmail());
            }

            if (((Authentication) principal).getAuthorities() instanceof Collection<?>) {
                authorities = ((Authentication) principal).getAuthorities();
            }
        }

        if (authorities != null && authorities.size() > 0) {
            Map<String, String> externalGroupMap = new HashMap<String, String>();
            int i = 0;
            for (GrantedAuthority authority : authorities) {
                externalGroupMap.put("externalGroups." + i, authority.getAuthority());
                i++;
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                mapper.writeValue(baos, externalGroupMap);
                appendField(login, "authorities", new String(baos.toByteArray()));
            } catch (Throwable t) {
                logger.error("Unable to convert external groups to be sent for authorization ", t);
            }
        }
        return login;
    }

    @RequestMapping(value = "/oauth/token", method = RequestMethod.POST, params = "grant_type=password")
    @ResponseBody
    public ResponseEntity<byte[]> tokenEndpoint(HttpServletRequest request, HttpEntity<byte[]> entity,
                    @RequestParam Map<String, String> parameters, Map<String, Object> model, Principal principal)
                    throws Exception {

        // Request has a password. Owner password grant with a UAA password
        if (null != request.getParameter("password")) {
            return passthru(request, entity, model, false);
        }
        else {
            //
            MultiValueMap<String, String> requestHeadersForClientInfo = new LinkedMaskingMultiValueMap<String, String>(
                            AUTHORIZATION);
            requestHeadersForClientInfo.add(AUTHORIZATION, request.getHeader(AUTHORIZATION));

            ResponseEntity<byte[]> clientInfoResponse = getDefaultTemplate().exchange(getUaaBaseUrl() + "/clientinfo",
                            HttpMethod.POST,
                            new HttpEntity<MultiValueMap<String, String>>(null, requestHeadersForClientInfo),
                            byte[].class);

            if (clientInfoResponse.getStatusCode() == HttpStatus.OK) {
                String path = extractPath(request);

                MultiValueMap<String, String> map = new LinkedMaskingMultiValueMap<String, String>();
                map.setAll(parameters);
                if (principal != null) {
                    map.set("source", "login");
                    map.set("add_new", String.valueOf(isAddNew()));
                    map.set("client_id", getClientId(clientInfoResponse.getBody()));
                    map.setAll(getLoginCredentials(principal));
                    map.remove("credentials"); // legacy vmc might break
                                               // otherwise
                }
                else {
                    throw new BadCredentialsException("No principal found in authorize endpoint");
                }

                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.putAll(getRequestHeaders(requestHeaders));
                requestHeaders.remove(AUTHORIZATION.toLowerCase());
                requestHeaders.remove(ACCEPT.toLowerCase());
                requestHeaders.remove(CONTENT_TYPE.toLowerCase());
                requestHeaders.setContentType(APPLICATION_FORM_URLENCODED);
                requestHeaders.setAccept(Arrays.asList(APPLICATION_JSON));
                requestHeaders.remove(COOKIE);
                requestHeaders.remove(COOKIE.toLowerCase());

                ResponseEntity<byte[]> response = getAuthorizationTemplate().exchange(getUaaBaseUrl() + "/" + path,
                                HttpMethod.POST, new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders),
                                byte[].class);

                saveCookie(response.getHeaders(), model);

                byte[] body = response.getBody();
                if (body != null) {
                    HttpHeaders outgoingHeaders = getResponseHeaders(response.getHeaders());
                    return new ResponseEntity<byte[]>(response.getBody(), outgoingHeaders, response.getStatusCode());
                }

                throw new IllegalStateException("Neither a redirect nor a user approval");
            }
            else {
                throw new BadCredentialsException(new String(clientInfoResponse.getBody()));
            }
        }
    }

    @RequestMapping(value = { "/passcode" }, method = RequestMethod.GET)
    public String generatePasscode(@RequestHeader HttpHeaders headers, Map<String, Object> model, Principal principal)
                    throws NoSuchAlgorithmException, IOException, JsonGenerationException, JsonMappingException {
        String username = null;
        Map<String, Object> authorizationParameters = null;

        if (principal instanceof ExpiringUsernameAuthenticationToken) {
            username = ((SamlUserDetails) ((ExpiringUsernameAuthenticationToken) principal).getPrincipal())
                            .getUsername();

            Collection<GrantedAuthority> authorities = ((SamlUserDetails) (((ExpiringUsernameAuthenticationToken) principal)
                            .getPrincipal())).getAuthorities();
            if (authorities != null) {
                authorizationParameters = new LinkedHashMap<String, Object>();
                authorizationParameters.put("authorities", authorities);
            }
        }
        else {
            username = principal.getName();
        }

        PasscodeInformation pi = new PasscodeInformation(username, null, authorizationParameters);

        ResponseEntity<ExpiringCode> response = doGenerateCode(pi);
        model.put("passcode", response.getBody().getCode());
        return "passcode";
    }

    private String getClientId(byte[] clientInfoResponse) {
        try {
            BaseClientDetails clientInfo = mapper.readValue(clientInfoResponse,
                            new TypeReference<BaseClientDetails>() {
                            });
            return clientInfo.getClientId();
        } catch (IOException e) {
            logger.warn("Unknown format of tokenRequest: " + new String(clientInfoResponse) + ". Ignoring.");
            return null;
        }
    }

}
