package org.cloudfoundry.identity.uaa.login;

public interface AccountCreationService {
    void beginActivation(String email);
}
