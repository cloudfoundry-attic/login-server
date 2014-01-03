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

import java.util.Map;

import org.cloudfoundry.identity.uaa.authentication.AuthzAuthenticationRequest;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthenticationDetails;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @author Dave Syer
 * 
 */
public class AutologinAuthenticationManager implements AuthenticationManager {

	private AutologinCodeStore codeStore = new DefaultAutologinCodeStore();

	/**
	 * @param codeStore the codeStore to set
	 */
	public void setCodeStore(AutologinCodeStore codeStore) {
		this.codeStore = codeStore;
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		if (!(authentication instanceof AuthzAuthenticationRequest)) {
			return null;
		}

		AuthzAuthenticationRequest request = (AuthzAuthenticationRequest) authentication;
		Map<String, String> info = request.getInfo();
		String code = info.get("code");
		Authentication user = codeStore.getUser(code);
		if (user == null) {
			throw new BadCredentialsException("Cannot redeem provided code for user");
		}
		
		//ensure that we stored clientId
		String clientId = (String)user.getDetails();
		if (clientId == null){
            throw new BadCredentialsException("Cannot redeem provided code for user, client id missing");
        }
		
		//validate the client Id
		if (!(authentication.getDetails() instanceof UaaAuthenticationDetails)) {
		    throw new BadCredentialsException("Cannot redeem provided code for user, auth details missing");
		}
		
		UaaAuthenticationDetails details = (UaaAuthenticationDetails)authentication.getDetails();
		if (!clientId.equals(details.getClientId())) {
		    throw new BadCredentialsException("Cannot redeem provided code for user, client mismatch");
		}
		

		UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(user, null,
				user.getAuthorities());
		result.setDetails(authentication.getDetails());
		return result;

	}

}
