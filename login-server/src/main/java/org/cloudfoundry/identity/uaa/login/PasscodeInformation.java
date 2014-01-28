package org.cloudfoundry.identity.uaa.login;

import java.util.Map;

public class PasscodeInformation {

	private String userId;
	private String passcode;
	private Map<String, Object> authorizationParameters;

	public PasscodeInformation(String userId, String passcode, Map<String, Object> authorizationParameters) {
		this.userId = userId;
		this.passcode = passcode;
		this.authorizationParameters = authorizationParameters;
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
