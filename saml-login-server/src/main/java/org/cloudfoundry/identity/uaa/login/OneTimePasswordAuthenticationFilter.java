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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Authentication filter to verify one time passwords with what's
 * cached in the one time password store.
 *
 * @author jdsa
 *
 */
public class OneTimePasswordAuthenticationFilter implements Filter {

	private final Log logger = LogFactory.getLog(getClass());

	private List<String> parameterNames = Collections.emptyList();

	private final Set<String> methods = Collections.singleton(HttpMethod.POST.toString());

	private final AuthenticationEntryPoint authenticationEntryPoint = new OAuth2AuthenticationEntryPoint();

	private final ObjectMapper mapper = new ObjectMapper();

	private OneTimePasswordStore store = null;

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		Map<String, String> loginInfo = getCredentials(req);

		String username = loginInfo.get("username");
		String password = loginInfo.get("password");

		if (loginInfo.isEmpty()) {
			throw new BadCredentialsException("Request does not contain credentials.");
		}
		else {
			logger.debug("Located credentials in request, with keys: " + loginInfo.keySet());
			if (methods != null && !methods.contains(req.getMethod().toUpperCase())) {
				throw new BadCredentialsException("Credentials must be sent by (one of methods): " + methods);
			}

			PasscodeInformation pi = store.validateOneTimePassword(new PasscodeInformation(username), password);
			if (pi != null) {
				logger.info("Successful authentication request for " + username);

				@SuppressWarnings("unchecked")
				Collection<GrantedAuthority> externalAuthorties = (Collection<GrantedAuthority>) pi.getAuthorizationParameters().get("authorities");

				Authentication result = new UsernamePasswordAuthenticationToken(username, null,
						externalAuthorties == null ? UaaAuthority.USER_AUTHORITIES : externalAuthorties);

				SecurityContextHolder.getContext().setAuthentication(result);
			}
			else {
				authenticationEntryPoint.commence(req, res, new BadCredentialsException("Invalid one time password"));
			}
		}

		chain.doFilter(request, response);
	}

	private Map<String, String> getCredentials(HttpServletRequest request) {
		Map<String, String> credentials = new HashMap<String, String>();

		for (String paramName : parameterNames) {
			String value = request.getParameter(paramName);
			if (value != null) {
				if (value.startsWith("{")) {
					try {
						Map<String, String> jsonCredentials = mapper.readValue(value,
								new TypeReference<Map<String, String>>() {
								});
						credentials.putAll(jsonCredentials);
					}
					catch (IOException e) {
						logger.warn("Unknown format of value for request param: " + paramName + ". Ignoring.");
					}
				}
				else {
					credentials.put(paramName, value);
				}
			}
		}

		return credentials;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void destroy() {
	}

	public void setParameterNames(List<String> parameterNames) {
		this.parameterNames = parameterNames;
	}

	public void setStore(OneTimePasswordStore store) {
		this.store = store;
	}

}