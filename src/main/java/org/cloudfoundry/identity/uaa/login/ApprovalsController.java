package org.cloudfoundry.identity.uaa.login;

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

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Vidya Valmikinathan
 */
@Controller
public class ApprovalsController implements InitializingBean {
	private static final Log logger = LogFactory.getLog(HomeController.class);

	private String approvalsUri;

	private String logoutUrl;

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

	public void setLogoutUrl(String logoutUrl) {
		this.logoutUrl = logoutUrl;
	}

	/**
	 * Display the current user's approvals
	 */
	@RequestMapping(value = "/approvals", method = RequestMethod.GET)
	public String get(Model model) {
		model.addAttribute("approvals", getCurrentApprovals());
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
		Assert.notNull("Supply a logout URL", logoutUrl);
	}
}
