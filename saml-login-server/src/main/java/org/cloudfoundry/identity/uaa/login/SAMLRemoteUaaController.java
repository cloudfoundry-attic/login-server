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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.identity.uaa.client.SocialClientUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

public class SAMLRemoteUaaController extends RemoteUaaController {

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
		if (principal instanceof ExpiringUsernameAuthenticationToken) {
			appendField(login, "username", ((SAMLUserDetails)(((ExpiringUsernameAuthenticationToken) principal).getPrincipal())).getUsername());

			Collection<GrantedAuthority> authorities = ((SAMLUserDetails)(((ExpiringUsernameAuthenticationToken) principal).getPrincipal())).getAuthorities();
			String[] authorityList = new String[authorities.size()];
			int i = 0;
			for (GrantedAuthority authority : authorities) {
				authorityList[i] = "\"externalGroups." + i + "\": \"" + authority.getAuthority() + "\"";
				i++;
			}
			appendField(login, "authorities", "{" + StringUtils.arrayToCommaDelimitedString(authorityList) + "}");
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
		}
		return login;
	}
}
