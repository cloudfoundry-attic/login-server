package org.cloudfoundry.identity.uaa.login;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.oauth.authz.Approval;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
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
	private static final Log logger = LogFactory.getLog(HomeController.class);

	private String approvalsUri;

	private Map<String,String> links = new HashMap<String, String>();
	
	private RestOperations restTemplate;

	public void setMapper(ObjectMapper mapper) {
		this.mapper = mapper;
	}

	private ObjectMapper mapper;

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
		Set<Approval> approvals = getCurrentApprovals();
		Set<Approval> denials = new HashSet<Approval>();
		for (Approval approval : approvals) {
			if (approval.getStatus() == Approval.ApprovalStatus.DENIED) {
				denials.add(approval);
			}
		}
		approvals.removeAll(denials);
		model.addAttribute("approvals", approvals);
		model.addAttribute("denials", denials);
		model.addAttribute("links", links );
		return "approvals";
	}

	private Set<Approval> getCurrentApprovals() {
		Set<Approval> approvals = Collections.emptySet();
		try {
			approvals = mapper.readValue(restTemplate.getForObject(approvalsUri, String.class), new TypeReference<Set<Approval>>() {
			});
		} catch (IOException e) {
			logger.error("Error parsing response from approvals enpoint", e);
		}
		return approvals;
	}

	/**
	 * Handle form post for revoking chosen approvals
	 */
	@RequestMapping(value = "/approvals", method = RequestMethod.POST)
	public String post(@RequestParam Map<String, String> params, Model model) {
		Set<Approval> approvals = getCurrentApprovals();
		Set<Approval> toRevoke = new HashSet<Approval>();
		for (Map.Entry<String, String> param : params.entrySet()) {
			try {
				toRevoke.add(mapper.readValue(param.getValue(), Approval.class));
			} catch (IOException e) {
				logger.warn(String.format("Error parsing request param: [%s] into Approval, moving on to next param", param.getValue()), e);
			}
		}
		approvals.removeAll(toRevoke);
		restTemplate.put(approvalsUri, approvals);

		return get(model);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull("Supply an approvals URI", approvalsUri);
	}
}
