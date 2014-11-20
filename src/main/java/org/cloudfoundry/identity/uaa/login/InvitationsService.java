package org.cloudfoundry.identity.uaa.login;

import java.util.List;

public interface InvitationsService {
    List<String> inviteUsers(List<String> emails, String currentUser);

    String acceptInvitation(String userId, String email, String password, String clientId);
}
