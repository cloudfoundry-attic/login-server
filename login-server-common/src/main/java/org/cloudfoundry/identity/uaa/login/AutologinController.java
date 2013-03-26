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
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.authentication.AuthzAuthenticationRequest;
import org.cloudfoundry.identity.uaa.client.SocialClientUserDetails;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Dave Syer
 * 
 */
@Controller
public class AutologinController {

	private static final Log logger = LogFactory.getLog(AutologinController.class);

	private AutologinCodeStore codeStore = new DefaultAutologinCodeStore();

	private AuthenticationManager authenticationManager;

	/**
	 * @param authenticationManager the authenticationManager to set
	 */
	public void setAuthenticationManager(AuthenticationManager authenticationManager) {
		this.authenticationManager = authenticationManager;
	}

	/**
	 * @param codeStore the codeStore to set
	 */
	public void setCodeStore(AutologinCodeStore codeStore) {
		this.codeStore = codeStore;
	}

	@RequestMapping(value = "/autologin", method = RequestMethod.POST)
	@ResponseBody
	public AutologinResponse generateAutologinCode(@RequestBody AutologinRequest request) throws Exception {
		String username = request.getUsername();
		if (username == null) {
			throw new BadCredentialsException("No username in request");
		}
		if (authenticationManager != null) {
			String password = request.getPassword();
			if (!StringUtils.hasText(password)) {
				throw new BadCredentialsException("No password in request");
			}
			authenticationManager.authenticate(new AuthzAuthenticationRequest(username, password, null));
		}
		logger.info("Autologin authentication request for " + username);
		SocialClientUserDetails user = new SocialClientUserDetails(username, UaaAuthority.USER_AUTHORITIES);
		return new AutologinResponse(codeStore.storeUser(user));
	}

	@JsonSerialize(include = Inclusion.NON_EMPTY)
	public static class AutologinRequest {

		private String username;

		private String password;

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		@Override
		public String toString() {
			return "AutologinRequest [username=" + username + ", password=" + password + "]";
		}

	}

	public static class AutologinResponse {

		private String code;

		public AutologinResponse(String code) {
			this.code = code;
		}

		public String getPath() {
			return "/oauth/authorize";
		}

		public String getCode() {
			return code;
		}

	}

	public static class AutologinRequestConverter extends AbstractHttpMessageConverter<AutologinRequest> {

		private FormHttpMessageConverter converter = new FormHttpMessageConverter();

		public AutologinRequestConverter() {
			setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED));
		}

		@Override
		protected boolean supports(Class<?> clazz) {
			return AutologinRequest.class.isAssignableFrom(clazz);
		}

		@Override
		protected AutologinRequest readInternal(Class<? extends AutologinRequest> clazz, HttpInputMessage inputMessage)
				throws IOException, HttpMessageNotReadableException {
			MultiValueMap<String, String> map = converter.read(null, inputMessage);
			String username = map.getFirst("username");
			String password = map.getFirst("password");
			AutologinRequest result = new AutologinRequest();
			result.setUsername(username);
			result.setPassword(password);
			return result;
		}

		@Override
		protected void writeInternal(AutologinRequest t, HttpOutputMessage outputMessage) throws IOException,
				HttpMessageNotWritableException {
			MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
			if (t.getUsername() != null) {
				map.set("username", t.getUsername());
			}
			if (t.getPassword() != null) {
				map.set("password", t.getPassword());
			}
			converter.write(map, MediaType.APPLICATION_FORM_URLENCODED, outputMessage);
		}
	}

}
