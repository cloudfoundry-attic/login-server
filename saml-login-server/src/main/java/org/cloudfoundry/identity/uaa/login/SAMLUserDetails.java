package org.cloudfoundry.identity.uaa.login;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

@SuppressWarnings("serial")
public class SAMLUserDetails extends User {

	public SAMLUserDetails(String username, String password, boolean enabled, boolean accountNonExpired,
			boolean credentialsNonExpired, boolean accountNonLocked,
			Collection<? extends GrantedAuthority> authorities) {
		super(username, password == null ? "" : password, enabled, accountNonExpired, credentialsNonExpired,
				accountNonLocked, authorities);
	}

}
