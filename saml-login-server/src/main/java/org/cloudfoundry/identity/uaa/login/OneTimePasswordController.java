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

	private PasscodeStore store = null;

	@RequestMapping(value = { "/passcode" }, method = RequestMethod.GET)
	public String generateOneTimePassword(@RequestHeader
	HttpHeaders headers, Map<String, Object> model, Principal principal) throws NoSuchAlgorithmException {

		String username = null;
		Map<String, Object> authorizationParameters = null;

		if (principal instanceof ExpiringUsernameAuthenticationToken) {
			username = ((SAMLUserDetails) ((ExpiringUsernameAuthenticationToken) principal).getPrincipal()).getUsername();

			Collection<GrantedAuthority> authorities = ((SAMLUserDetails) (((ExpiringUsernameAuthenticationToken) principal)
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
		model.put("oneTimePassword", store.getPasscode(pi));

		return "passcode";
	}

	public void setStore(PasscodeStore store) {
		this.store = store;
	}
}
