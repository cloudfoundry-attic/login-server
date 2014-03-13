package org.cloudfoundry.identity.uaa.login;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.springframework.security.core.GrantedAuthority;

@SuppressWarnings("serial")
public class SamlUserAuthority implements GrantedAuthority{

	private final String authority;
	
	@JsonCreator
	public SamlUserAuthority(@JsonProperty("authority") String authority) {
		this.authority = authority;
	}

	@Override
	public String getAuthority() {
		return authority;
	}

}
