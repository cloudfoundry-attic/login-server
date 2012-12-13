package org.cloudfoundry.identity.uaa.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestOperations;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
public class ApprovalsController implements InitializingBean {
	private static final Log logger = LogFactory.getLog(HomeController.class);

	private String approvalsUri;

	private String logoutUrl;

	private RestOperations restTemplate;

	public ApprovalsController(RestOperations restTemplate) {
		this.restTemplate = restTemplate;
	}

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
		List approvals = restTemplate.getForEntity(approvalsUri, List.class).getBody();

		Map<String, Set<String>> approvedScopes = new HashMap<String, Set<String>>();
		for (Object approval : approvals) {
			Map<String, Object> app = (Map<String, Object>) approval;
			String clientId = (String) app.get("clientId");
			String scope = (String) app.get("scope");
			Date expiresAt = new Date(Long.parseLong(app.get("expiresAt").toString()));

			if (!approvedScopes.containsKey(clientId)) {
				approvedScopes.put(clientId, new HashSet<String>());
			}
			if (expiresAt.after(new Date())) {
				approvedScopes.get(clientId).add(scope);
			}
		}
		model.addAttribute("clients", StringUtils.collectionToCommaDelimitedString(approvedScopes.keySet()));
		for (String client : approvedScopes.keySet()) {
			model.addAttribute(client, StringUtils.collectionToCommaDelimitedString(approvedScopes.get(client)));
		}
		model.addAttribute("logoutUrl", logoutUrl);

		return "approvals";
	}

	@RequestMapping(value = "/approvals", method = RequestMethod.POST)
	public String post(HttpServletRequest request, Model model) {
		List approvals = restTemplate.getForEntity(approvalsUri, List.class).getBody();
		List toRevoke = new ArrayList();

		for (Object reqParamName : Collections.list(request.getParameterNames())) {
			String paramName = (String) reqParamName;
			if ("on".equalsIgnoreCase(request.getParameter(paramName))) {
				int delimIndex = paramName.indexOf(".");
				String clientIdToRevoke = paramName.substring(0, delimIndex);
				String scopeToRevoke = paramName.substring(delimIndex+1);
				for (Object approval : approvals) {
					Map<String, Object> app = (Map<String, Object>) approval;
					String clientId = (String) app.get("clientId");
					String scope = (String) app.get("scope");
					Date expiresAt = new Date(Long.parseLong(app.get("expiresAt").toString()));

					if (clientId.equals(clientIdToRevoke) && scope.equals(scopeToRevoke) && expiresAt.after(new Date())) {
						toRevoke.add(approval);
					}
				}
			}
		}
		logger.debug("Revoking approvals: " + toRevoke);
		approvals.removeAll(toRevoke);
		restTemplate.put(approvalsUri, approvals);

		return get(model);
	}

//	private List<Map<String, String>> parseApprovalsJson (List approvals) {
//		List<Map<String, String>> response = new ArrayList<Approval>();
//		for (Object approval : approvals) {
//			Map<String, Object> app = (Map<String, Object>) approval;
//			String userId = (String) app.get("userName");
//			String clientId = (String) app.get("clientId");
//			String scope = (String) app.get("scope");
//			Date expiresAt = new Date(Long.parseLong(app.get("expiresAt").toString()));
//
//			if () {
//				response.add(new Approval(userId, clientId, scope, expiresAt));
//			}
//		}
//		return response;
//	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull("Supply an approvals URI", approvalsUri);
		Assert.notNull("Supply a logout URL", logoutUrl);
	}
}
