package org.cloudfoundry.identity.uaa.login;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

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

}
