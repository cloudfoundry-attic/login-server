package org.cloudfoundry.identity.uaa.login;

import org.springframework.security.core.GrantedAuthority;

@SuppressWarnings("serial")
public class SamlUserAuthority implements GrantedAuthority{

	private final String authority;

	public SamlUserAuthority(String authority) {
		this.authority = authority;
	}

	@Override
	public String getAuthority() {
		return authority;
	}

}
