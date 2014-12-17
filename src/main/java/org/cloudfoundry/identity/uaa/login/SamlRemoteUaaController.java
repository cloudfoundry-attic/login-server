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
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthenticationDetails;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.client.SocialClientUserDetails;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.cloudfoundry.identity.uaa.login.saml.IdentityProviderDefinition;
import org.cloudfoundry.identity.uaa.login.saml.LoginSamlAuthenticationToken;
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
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
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

    public static final String NotANumber = "NaN";

    private final ObjectMapper mapper = new ObjectMapper();

    public void setIdpDefinitions(List<IdentityProviderDefinition> idpDefinitions) {
        this.idpDefinitions = idpDefinitions;
    }

    private List<IdentityProviderDefinition> idpDefinitions;

    @Value("${login.entityID}")
    public String entityID = "";

    public SamlRemoteUaaController(Environment environment, RestTemplate restTemplate) {
        super(environment, restTemplate);
    }

    @Override
    @RequestMapping(value = { "/info", "/login" }, method = RequestMethod.GET, produces = APPLICATION_JSON_VALUE, headers = "Accept=application/json")
    public String prompts(HttpServletRequest request, @RequestHeader HttpHeaders headers, Map<String, Object> model,
                    Principal principal) throws Exception {

        // Entity ID to start the discovery
        model.put("entityID", entityID);
        model.put("idpDefinitions", idpDefinitions);
        for (IdentityProviderDefinition idp : idpDefinitions) {
            if(idp.isShowSamlLink()) {
                model.put("showSamlLoginLinks", true);
                break;
            }
        }
        return super.prompts(request, headers, model, principal);
    }

    @RequestMapping(value = { "/info", "/login" }, method = RequestMethod.GET, produces = TEXT_HTML_VALUE, headers = "Accept=text/html, */*")
    public String samlUiPrompts(HttpServletRequest request, @RequestHeader HttpHeaders headers, Map<String, Object> model,
                          Principal principal) throws Exception {

        String logicalViewName = prompts(request, headers, model, principal);

        Map<String,Object> prompts = new LinkedHashMap<String, Object>((Map<String, Object>) model.get("prompts"));
        prompts.remove("passcode");
        // Entity ID to start the discovery
        model.put("entityID", entityID);
        model.put("idpDefinitions", idpDefinitions);
        model.put("prompts", prompts);

        return logicalViewName;
    }

    @Override
    protected Map<String, String> getLoginCredentials(Principal principal) {
        Map<String, String> login = super.getLoginCredentials(principal);
        Collection<? extends GrantedAuthority> authorities = null;

        if (principal instanceof LoginSamlAuthenticationToken) {
            appendField(login, UaaAuthenticationDetails.ADD_NEW, "true");
            LoginSamlAuthenticationToken et = (LoginSamlAuthenticationToken)principal;
            appendField(login, Origin.ORIGIN, et.getIdpAlias());
            if (et.getPrincipal() instanceof String ) {
                appendField(login, "username", et.getPrincipal());
                authorities = et.getAuthorities();
            } else if (et.getPrincipal() instanceof UaaPrincipal) {
                appendField(login, "username", ((UaaPrincipal)et.getPrincipal()).getName());
                appendField(login, "email", ((UaaPrincipal)et.getPrincipal()).getEmail());
                appendField(login, "external_id", ((UaaPrincipal)et.getPrincipal()).getExternalId());
                authorities = et.getAuthorities();
            } else {
                appendField(login, "username",
                    ((SamlUserDetails) (((ExpiringUsernameAuthenticationToken) principal).getPrincipal()))
                        .getUsername());
                authorities = ((SamlUserDetails) (((ExpiringUsernameAuthenticationToken) principal).getPrincipal()))
                    .getAuthorities();
            }
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

    @RequestMapping(value = { "/passcode" }, method = RequestMethod.GET)
    public String generatePasscode(@RequestHeader HttpHeaders headers, Map<String, Object> model, Principal principal)
                    throws NoSuchAlgorithmException, IOException, JsonMappingException {
        String username = null, origin = null;
        Map<String, Object> authorizationParameters = null;

        if (principal instanceof LoginSamlAuthenticationToken) {
            username = principal.getName();
            origin = ((LoginSamlAuthenticationToken)principal).getIdpAlias();
            //TODO collect authorities here?
        } else if (principal instanceof ExpiringUsernameAuthenticationToken) {
            username = ((SamlUserDetails) ((ExpiringUsernameAuthenticationToken) principal).getPrincipal()).getUsername();
            origin = "login-saml";
            Collection<GrantedAuthority> authorities = ((SamlUserDetails) (((ExpiringUsernameAuthenticationToken) principal)
                            .getPrincipal())).getAuthorities();
            if (authorities != null) {
                authorizationParameters = new LinkedHashMap<>();
                authorizationParameters.put("authorities", authorities);
            }
        } else {
            username = principal.getName();
            origin = "passcode";
        }

        PasscodeInformation pi = new PasscodeInformation(NotANumber, username, null, origin, authorizationParameters);

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
