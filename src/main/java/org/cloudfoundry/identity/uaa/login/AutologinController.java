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
import org.cloudfoundry.identity.uaa.social.SocialClientUserDetails;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
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

	/**
	 * @param codeStore the codeStore to set
	 */
	public void setCodeStore(AutologinCodeStore codeStore) {
		this.codeStore = codeStore;
	}

	@RequestMapping(value = "/autologin", method = RequestMethod.POST)
	@ResponseBody
	public AutologinResponse implicit(@RequestBody AutologinRequest request) throws Exception {
		String username = request.getUsername();
		if (username==null) {
			throw new BadCredentialsException("No username in request");
		}
		logger.info("Autologin authentication request for " + username);
		SocialClientUserDetails user = new SocialClientUserDetails(username, UaaAuthority.USER_AUTHORITIES);
		return new AutologinResponse(codeStore.storeUser(user));
	}

	public static class AutologinRequest {

		private String username;

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
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

}
