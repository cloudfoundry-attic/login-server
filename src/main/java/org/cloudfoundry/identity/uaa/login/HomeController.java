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

	@RequestMapping("/error500")
	public String error500(Model model) {
		model.addAttribute("error", "Something went wrong. Please try again later.");
		return "error";
	}

	@RequestMapping("/error404")
	public String error404(Model model) {
		model.addAttribute("error", "That page couldn't be found.");
		return "error";
	}

}
