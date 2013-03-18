package org.cloudfoundry.identity.uaa.login;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cloudfoundry.identity.uaa.client.SocialClientUserDetails;
import org.opensaml.saml2.core.impl.NameIDImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


public class SAMLRemoteUaaController extends RemoteUaaController {

    @Value("${login.entityID}")
    public String entityID = "";

    @Override
	@RequestMapping(value = { "/info", "/login" }, method = RequestMethod.GET)
	public String prompts(HttpServletRequest request, @RequestHeader HttpHeaders headers, Map<String, Object> model,
			Principal principal) throws Exception {
		model.put("entityID", entityID);
		return super.prompts(request, headers, model, principal);
	}

    @Override
	protected Map<String, String> getLoginCredentials(Principal principal) {
		Map<String, String> login = new LinkedHashMap<String, String>();
		if (principal instanceof ExpiringUsernameAuthenticationToken) {
			appendField(login, "username", ((NameIDImpl)((ExpiringUsernameAuthenticationToken)principal).getPrincipal()).getValue());
		} else {
			appendField(login, "username", principal.getName());
		}
		if (principal instanceof Authentication) {
			Object details = ((Authentication) principal).getPrincipal();
			if (details instanceof SocialClientUserDetails) {
				SocialClientUserDetails user = (SocialClientUserDetails) details;
				appendField(login, "name", user.getName());
				appendField(login, "external_id", user.getExternalId());
				appendField(login, "email", user.getEmail());
			}
		}
		return login;
	}
}
