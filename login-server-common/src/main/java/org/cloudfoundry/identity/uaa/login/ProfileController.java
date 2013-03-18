package org.cloudfoundry.identity.uaa.login;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
public class ProfileController implements InitializingBean {

	private String approvalsUri;

	private Map<String, String> links = new HashMap<String, String>();

	private RestOperations restTemplate;

	private final Log logger = LogFactory.getLog(getClass());

	public ProfileController(RestOperations restTemplate) {
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
	@RequestMapping(value = "/profile", method = RequestMethod.GET)
	public String get(Model model) {
		Map<String, List<Object>> approvals = getCurrentApprovals();
		model.addAttribute("approvals", approvals);
		model.addAttribute("links", links);
		return "approvals";
	}

	private Map<String, List<Object>> getCurrentApprovals() {
		// Result will be a map of <clientId, approvalInfo>
		Map<String, List<Object>> result = new LinkedHashMap<String, List<Object>>();
		@SuppressWarnings("unchecked")
		Set<Map<String, Object>> approvals = restTemplate.getForObject(approvalsUri, Set.class);
		for (Map<String, Object> approvalMap : approvals) {
			String clientId = (String) approvalMap.get("clientId");

			List<Object> approvalList = result.get(clientId);
			if (null == approvalList) {
				approvalList = new ArrayList<Object>();
			}

			approvalList.add(approvalMap);

			result.put(clientId, approvalList);
		}
		return result;
	}

	/**
	 * Handle form post for revoking chosen approvals
	 */
	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public String post(@RequestParam(required = false) Collection<String> checkedScopes,
					   @RequestParam(required = false) String update,
					   @RequestParam(required = false) String delete,
					   @RequestParam(required = false) String clientId, Model model) {

		if (null != update) {
			Map<String, List<Object>> approvals = getCurrentApprovals();

			List<Object> allApprovals = new ArrayList<Object>();
			for (List<Object> clientApprovals : approvals.values()) {
				allApprovals.addAll(clientApprovals);
			}

			List<Object> updatedApprovals = new ArrayList<Object>();
			for (Object approval : allApprovals) {
				@SuppressWarnings("unchecked")
				Map<String, String> approvalToBeUpdated = new HashMap<String, String>((Map<String, String>) approval);
				if (checkedScopes != null
						&& checkedScopes.contains(approvalToBeUpdated.get("clientId") + "-"
								+ approvalToBeUpdated.get("scope"))) {
					approvalToBeUpdated.put("status", "APPROVED");
				}
				else {
					approvalToBeUpdated.put("status", "DENIED");
				}
				updatedApprovals.add(approvalToBeUpdated);
			}

			restTemplate.put(approvalsUri, updatedApprovals);
		}
		else if (null != delete) {
			deleteApprovalsForClient(clientId);
		}

		return get(model);
	}

	private void deleteApprovalsForClient(String clientId) {
		ResponseEntity<String> response = restTemplate.exchange(approvalsUri + "?clientId=" + clientId,
				HttpMethod.DELETE, null, String.class);
		logger.debug("Delete approvals request for client " + clientId + " resulted in " + response);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull("Supply an approvals URI", approvalsUri);
	}
}
