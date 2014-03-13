package org.cloudfoundry.identity.uaa.login;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
//TODO - make object serialize/deserialize properly with JSON
public class PasscodeInformation {

	private String userId;
	private String passcode;
	private Map<String, Object> authorizationParameters;
	
	public PasscodeInformation(String userId, 
	                           String passcode, 
	                           Map<String, Object> authorizationParameters) {
		setUserId(userId);
		setPasscode(passcode);
		setAuthorizationParameters(authorizationParameters);
	}

	@JsonCreator
	public PasscodeInformation(@JsonProperty("userId") String userId, 
	                           @JsonProperty("passcode") String passcode, 
	                           @JsonProperty("samlAuthorities") ArrayList<SamlUserAuthority> authorities) {
	    setUserId(userId);
	    setPasscode(passcode);
	    authorizationParameters = new LinkedHashMap<String, Object>();
	    setSamlAuthorities(authorities);
	}


	public PasscodeInformation(String userId) {
		this.userId = userId;
	}

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	
	@JsonProperty("samlAuthorities")
	public ArrayList<SamlUserAuthority> getSamlAuthorities() {
	    ArrayList<SamlUserAuthority> list = new ArrayList<SamlUserAuthority>();
	    if (authorizationParameters!=null && authorizationParameters.containsKey("authorities")) {
    	    Set<SamlUserAuthority> set = (Set<SamlUserAuthority>)authorizationParameters.get("authorities");
    	    list.addAll(set);
	    }
	    return list;
	}
	
    public void setSamlAuthorities(ArrayList<SamlUserAuthority> authorities) {
	    Set<SamlUserAuthority> set = new HashSet<SamlUserAuthority>();
	    set.addAll(authorities);
	    authorizationParameters.put("authorities", set);
	}
	
	@JsonIgnore
	public Map<String, Object> getAuthorizationParameters() {
		return authorizationParameters;
	}
	public void setAuthorizationParameters(Map<String, Object> authorizationParameters) {
		this.authorizationParameters = authorizationParameters;
	}
	public String getPasscode() {
		return passcode;
	}
	public void setPasscode(String passcode) {
		this.passcode = passcode;
	}
}
