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

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.client.SocialClientUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

public class SAMLRemoteUaaController extends RemoteUaaController {

	private static final Log logger = LogFactory.getLog(SAMLRemoteUaaController.class);

	@Value("${login.entityID}")
	public String entityID = "";

	@Override
	@RequestMapping(value = { "/info", "/login" }, method = RequestMethod.GET)
	public String prompts(HttpServletRequest request, @RequestHeader
	HttpHeaders headers, Map<String, Object> model, Principal principal) throws Exception {
		// Entity ID to start the discovery
		model.put("entityID", entityID);
		return super.prompts(request, headers, model, principal);
	}

	@Override
	protected Map<String, String> getLoginCredentials(Principal principal) {
		Map<String, String> login = new LinkedHashMap<String, String>();
		Collection<? extends GrantedAuthority> authorities = null;

		if (principal instanceof ExpiringUsernameAuthenticationToken) {
			appendField(login, "username",
					((SAMLUserDetails) (((ExpiringUsernameAuthenticationToken) principal).getPrincipal()))
							.getUsername());

			authorities = ((SAMLUserDetails) (((ExpiringUsernameAuthenticationToken) principal).getPrincipal())).getAuthorities();
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

			if (((Authentication)principal).getAuthorities() instanceof Collection<?>) {
				authorities = ((Authentication)principal).getAuthorities();
			}
		}

		if (authorities != null) {
			String[] authorityList = new String[authorities.size()];
			int i = 0;
			for (GrantedAuthority authority : authorities) {
				authorityList[i] = "\"externalGroups." + i + "\": \"" + authority.getAuthority() + "\"";
				i++;
			}
			appendField(login, "authorities", "{" + StringUtils.arrayToCommaDelimitedString(authorityList) + "}");
		}
		return login;
	}

	@RequestMapping(value = "/oauth/token", method = RequestMethod.POST, params = "grant_type=password" )
	@ResponseBody
	public ResponseEntity<byte[]> tokenEndpoint(HttpServletRequest request, HttpEntity<byte[]> entity,
			Map<String, Object> model, Principal principal) throws Exception {


		MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
		if (principal != null) {
			map.set("source", "login");
			map.setAll(getLoginCredentials(principal));
			map.remove("credentials"); // cf might break otherwise
		}
		else {
			throw new BadCredentialsException("No principal found in authorize endpoint");
		}

		HttpHeaders requestHeaders = new HttpHeaders();
//		requestHeaders.putAll(getRequestHeaders(headers));
		requestHeaders.remove(AUTHORIZATION.toLowerCase());
		requestHeaders.remove(ACCEPT.toLowerCase());
		requestHeaders.remove(CONTENT_TYPE.toLowerCase());
		requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		requestHeaders.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		requestHeaders.remove(COOKIE);
		requestHeaders.remove(COOKIE.toLowerCase());

		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> response;

		response = getAuthorizationTemplate().exchange(getUaaBaseUrl() + "/oauth/authorize", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, String>>(map, requestHeaders), Map.class);

		saveCookie(response.getHeaders(), model);

		@SuppressWarnings("unchecked")
		Map<String, Object> body = response.getBody();
		if (body != null) {
			// User approval is required
			logger.debug("Response: " + body);
			throw new InvalidTokenException("Some scopes were not granted");
		}

//		String location = response.getHeaders().getFirst("Location");
//		if (location != null) {
//			logger.info("Redirect in /oauth/authorize for: " + principal.getName());
//			// Don't expose model attributes (cookie) in redirect
//			return new ModelAndView(new RedirectView(location, false, true, false));
//		}
//
//		throw new IllegalStateException("Neither a redirect nor a user approval");


//		return passthru(request, entity, model);

		return new ResponseEntity<byte[]>(response.getBody(), outgoingHeaders, response.getStatusCode());
	}

}
