package org.cloudfoundry.identity.uaa.login;

public interface AccountCreationService {
    void beginActivation(String email);

    String completeActivation(String code, String password);
}
