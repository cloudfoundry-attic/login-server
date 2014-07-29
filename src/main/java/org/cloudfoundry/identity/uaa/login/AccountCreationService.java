package org.cloudfoundry.identity.uaa.login;

import org.codehaus.jackson.annotate.JsonProperty;

public interface AccountCreationService {
    void beginActivation(String email, String clientId);

    AccountCreation completeActivation(String code, String password);

    public static class AccountCreation {
        @JsonProperty("user_id")
        private String userId;
        private String username;
        @JsonProperty("redirect_location")
        private String redirectLocation;

        public AccountCreation() {}

        public AccountCreation(String userId, String username, String redirectLocation) {
            this.userId = userId;
            this.username = username;
            this.redirectLocation = redirectLocation;
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

        public String getRedirectLocation() {
            return redirectLocation;
        }

        public void setRedirectLocation(String redirectLocation) {
            this.redirectLocation = redirectLocation;
        }
    }
}
