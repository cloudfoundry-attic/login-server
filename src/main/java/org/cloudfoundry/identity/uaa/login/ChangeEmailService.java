package org.cloudfoundry.identity.uaa.login;

import java.util.Map;

public interface ChangeEmailService {

    public void beginEmailChange(String userId, String userEmail, String newEmail);

    public Map<String, String> completeVerification(String code);

}
