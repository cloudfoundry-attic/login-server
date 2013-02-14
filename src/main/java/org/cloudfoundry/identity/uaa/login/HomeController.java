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

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
	private final Log logger = LogFactory.getLog(getClass());
	private Map<String,String> links = new HashMap<String, String>();

	/**
	 * @param links the links to set
	 */
	public void setLinks(Map<String, String> links) {
		this.links = links;
	}

	@RequestMapping(value={"/", "/home"})
	public String home(Model model, Principal principal) {
		model.addAttribute("principal", principal);
		model.addAttribute("links", links );
		return "home";
	}

	@RequestMapping("/error500")
	public String error500(Model model, HttpServletRequest request) {
		logger.error("Internal error", (Throwable) request.getAttribute("javax.servlet.error.exception"));

		model.addAttribute("error", "Something went wrong. Please try again later.");
		return "error";
	}

	@RequestMapping("/error404")
	public String error404(Model model) {
		model.addAttribute("error", "That page couldn't be found.");
		return "error";
	}

}
