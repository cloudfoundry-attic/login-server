package org.cloudfoundry.identity.uaa.login;

import org.codehaus.jackson.annotate.JsonProperty;

public interface AccountCreationService {
    void beginActivation(String email);

    Account completeActivation(String code, String password);

    public static class Account {
        @JsonProperty("user_id")
        private String userId;
        private String username;

        public Account() {}

        public Account(String userId, String username) {
            this.userId = userId;
            this.username = username;
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
    }
}
