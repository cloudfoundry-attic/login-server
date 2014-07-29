package org.cloudfoundry.identity.uaa.login;

import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Configuration
public class LoginServerConfig {

    @Bean
    @Conditional(CreateAccountCondition.class)
    public AccountsController accountsController(AccountCreationService accountCreationService) {
        return new AccountsController(accountCreationService);
    }

    public static class CreateAccountCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return !"false".equalsIgnoreCase(context.getEnvironment().getProperty("login.signupsEnabled"));
        }
    }
}
