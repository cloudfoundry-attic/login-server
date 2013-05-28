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

import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 * @author jdsa
 *
 */
@Controller
public class OneTimePasswordController {

	private OneTimePasswordStore store = null;

	@RequestMapping(value = { "/passcode" }, method = RequestMethod.GET)
	public String generateOneTimePassword(@RequestHeader
	HttpHeaders headers, Map<String, Object> model, Principal principal) throws NoSuchAlgorithmException {

		String username = null;
		if (principal instanceof ExpiringUsernameAuthenticationToken) {
			username = ((SAMLUserDetails) ((ExpiringUsernameAuthenticationToken) principal).getPrincipal()).getUsername();
		}
		else {
			username = principal.getName();
		}

		Collection<GrantedAuthority> authorities = ((SAMLUserDetails) (((ExpiringUsernameAuthenticationToken) principal)
				.getPrincipal())).getAuthorities();
		Map<String, Object> authorizationParameters = null;
		if (authorities != null) {
			String[] authorityList = new String[authorities.size()];
			int i = 0;
			for (GrantedAuthority authority : authorities) {
				authorityList[i] = "\"externalGroups." + i + "\": \"" + authority.getAuthority() + "\"";
				i++;
			}
			authorizationParameters = new LinkedHashMap<String, Object>();
//			authorizationParameters.put("authorities", "{" + StringUtils.arrayToCommaDelimitedString(authorityList) + "}");
			authorizationParameters.put("authorities", authorities);
		}

		PasscodeInformation pi = new PasscodeInformation(username, null, authorizationParameters);
		model.put("oneTimePassword", store.getOneTimePassword(pi));

		return "one_time_code";
	}

	public void setStore(OneTimePasswordStore store) {
		this.store = store;
	}
}
