package org.cloudfoundry.identity.uaa.login;

public interface InvitationsService {
    void sendInviteEmail(String email, String currentUser);
}
