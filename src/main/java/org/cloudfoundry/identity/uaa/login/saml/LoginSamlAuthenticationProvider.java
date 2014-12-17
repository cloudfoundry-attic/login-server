/*******************************************************************************
 *     Cloud Foundry
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.login.saml;


import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.login.RemoteUaaAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.providers.ExpiringUsernameAuthenticationToken;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLAuthenticationToken;
import org.springframework.security.saml.context.SAMLMessageContext;

public class LoginSamlAuthenticationProvider extends SAMLAuthenticationProvider {

    private RemoteUaaAuthenticationManager authenticationManager;

    public RemoteUaaAuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public void setAuthenticationManager(RemoteUaaAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            throw new IllegalArgumentException("Only SAMLAuthenticationToken is supported, " + authentication.getClass() + " was attempted");
        }
        SAMLAuthenticationToken token = (SAMLAuthenticationToken) authentication;
        SAMLMessageContext context = token.getCredentials();
        String alias = context.getPeerExtendedMetadata().getAlias();
        ExpiringUsernameAuthenticationToken result = (ExpiringUsernameAuthenticationToken)super.authenticate(authentication);
        UaaPrincipal principal = new UaaPrincipal("NaN", result.getName(), result.getName(), alias, result.getName());
        result = new ExpiringUsernameAuthenticationToken(result.getTokenExpiration(), principal, result.getCredentials(), result.getAuthorities());
        Authentication auth = getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(principal, null, result.getAuthorities()));
        //TODO - Consolidate the different authentication objects we actually store in memory
        if (auth.getPrincipal() instanceof UaaPrincipal) {
            principal = new UaaPrincipal(((UaaPrincipal)auth.getPrincipal()).getId(), result.getName(), result.getName(), alias, result.getName());
        }
        result = new ExpiringUsernameAuthenticationToken(result.getTokenExpiration(), principal, result.getCredentials(), result.getAuthorities());
        LoginSamlAuthenticationToken samlAuthenticationToken = new LoginSamlAuthenticationToken(result, alias);
        return samlAuthenticationToken;
    }
}
