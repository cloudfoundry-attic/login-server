package org.cloudfoundry.identity.uaa.login;

import org.codehaus.jackson.annotate.JsonProperty;

import java.io.IOException;

public interface InvitationsService {
    void inviteUser(String email, String currentUser);

    InvitationAcceptanceResponse acceptInvitation(String email, String password, String code) throws ExpiringCodeService.CodeNotFoundException, IOException;

    public static class InvitationAcceptanceResponse {

        @JsonProperty("user_id")
        private String userId;
        private String username;
        private String email;

        public InvitationAcceptanceResponse(String userId, String username, String email) {
            this.userId = userId;
            this.username = username;
            this.email = email;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
