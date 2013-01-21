package org.cloudfoundry.identity.uaa.login;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestOperations;

/**
 * @author Vidya Valmikinathan
 */
@Controller
public class ApprovalsController implements InitializingBean {

	private String approvalsUri;

	private Map<String, String> links = new HashMap<String, String>();

	private RestOperations restTemplate;

	public ApprovalsController(RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * The URI for the user's approvals
	 * @param approvalsUri
	 */
	public void setApprovalsUri(String approvalsUri) {
		this.approvalsUri = approvalsUri;
	}

	/**
	 * @param links the links to set
	 */
	public void setLinks(Map<String, String> links) {
		this.links = links;
	}

	/**
	 * Display the current user's approvals
	 */
	@RequestMapping(value = "/approvals", method = RequestMethod.GET)
	public String get(Model model) {
		Map<String, Map<String, Object>> approvals = getCurrentApprovals();
		Set<Map<String, Object>> denials = new HashSet<Map<String, Object>>();
		for (String key : approvals.keySet()) {
			Map<String, Object> approval = approvals.get(key);
			approval.put("key", key);
			if (approval.get("status") == "DENIED") {
				denials.add(approval);
				approvals.remove(key);
			}
		}
		model.addAttribute("approvals", approvals.values());
		model.addAttribute("denials", denials);
		model.addAttribute("links", links);
		return "approvals";
	}

	private Map<String, Map<String, Object>> getCurrentApprovals() {
		Map<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>();
		@SuppressWarnings("unchecked")
		Set<Map<String, Object>> approvals = restTemplate.getForObject(approvalsUri, Set.class);
		for (Map<String, Object> map : approvals) {
			String key = map.get("clientId") + ":" + map.get("scope");
			result.put(key, map);
		}
		return result;
	}

	/**
	 * Handle form post for revoking chosen approvals
	 */
	@RequestMapping(value = "/approvals", method = RequestMethod.POST)
	public String post(@RequestParam Collection<String> revokes, Model model) {
		Map<String, Map<String, Object>> approvals = getCurrentApprovals();
		for (String key : revokes) {
			approvals.remove(key);
		}
		restTemplate.put(approvalsUri, approvals.values());

		return get(model);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull("Supply an approvals URI", approvalsUri);
	}
}
