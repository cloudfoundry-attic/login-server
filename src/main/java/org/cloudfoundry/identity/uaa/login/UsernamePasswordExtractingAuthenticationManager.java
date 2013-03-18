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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * Authentication filter translating a generic Authentication into a UsernamePasswordAuthenticationToken.
 * 
 * @author Dave Syer
 * 
 */
public class UsernamePasswordExtractingAuthenticationManager implements AuthenticationManager {

	protected final Log logger = LogFactory.getLog(getClass());

	private final AuthenticationManager delegate;
	
	/**
	 * @param delegate
	 */
	public UsernamePasswordExtractingAuthenticationManager(AuthenticationManager delegate) {
		super();
		this.delegate = delegate;
	}

	/* (non-Javadoc)
	 * @see org.springframework.security.authentication.AuthenticationManager#authenticate(org.springframework.security.core.Authentication)
	 */
	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		if (authentication == null || authentication instanceof UsernamePasswordAuthenticationToken) {
			return authentication;
		}
		
		UsernamePasswordAuthenticationToken output = new UsernamePasswordAuthenticationToken(authentication, authentication.getCredentials(), authentication.getAuthorities());
		output.setAuthenticated(authentication.isAuthenticated());
		output.setDetails(authentication.getDetails());
		return delegate.authenticate(output);

	}

}
