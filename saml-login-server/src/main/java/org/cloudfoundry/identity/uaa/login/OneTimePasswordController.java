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
import java.util.Map;

import org.opensaml.saml2.core.impl.NameIDImpl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class OneTimePasswordController {

	private OneTimePasswordStore store = null;

	@RequestMapping(value = { "/generate_code" }, method = RequestMethod.GET)
	public String generateOneTimePassword(@RequestHeader
	HttpHeaders headers, Map<String, Object> model, Principal principal) throws NoSuchAlgorithmException {

		String username = null;
		if (principal instanceof ExpiringUsernameAuthenticationToken) {
			username = ((NameIDImpl) ((ExpiringUsernameAuthenticationToken) principal).getPrincipal()).getValue();
		}
		else {
			username = principal.getName();
		}
		model.put("oneTimePassword", store.getOneTimePassword(username));

		return "one_time_code";
	}

	public void setStore(OneTimePasswordStore store) {
		this.store = store;
	}
}
