package org.cloudfoundry.identity.uaa.login;

import java.util.regex.Pattern;

public class EmailValidator {
	
	private static final Pattern emailRegex = Pattern.compile("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
			+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");
	public static boolean isValid(String email) {
		return emailRegex.matcher(email).matches();
	}

}
