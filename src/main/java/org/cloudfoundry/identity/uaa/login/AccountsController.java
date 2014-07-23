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
package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RequestMapping("/accounts")
public class AccountsController {

    private final AccountCreationService accountCreationService;

    public AccountsController(AccountCreationService accountCreationService) {
        this.accountCreationService = accountCreationService;
    }

    @RequestMapping(value = "/new", method = GET)
    public String activationEmail(Model model,
                                  @RequestParam(value = "client_id", defaultValue = "login") String clientId) {
        model.addAttribute("client_id", clientId);
        return "accounts/new_activation_email";
    }

    @RequestMapping(value = "/new", method = GET, params = {"code", "email"})
    public String newAccount() {
        return "accounts/new";
    }

    @RequestMapping(method = POST, params = {"email", "client_id"})
    public String sendActivationEmail(@RequestParam("email") String email,
                                      @RequestParam("client_id") String clientId) {
        accountCreationService.beginActivation(email, clientId);
        return "redirect:accounts/email_sent";
    }

    @RequestMapping(value = "/email_sent", method = RequestMethod.GET)
    public String emailSent() {
        return "accounts/email_sent";
    }

    @RequestMapping(method = POST, params = {"email", "code", "password", "password_confirmation"})
    public String createAccount(Model model,
                                @RequestParam("code") String code,
                                @RequestParam("password") String password,
                                @RequestParam("password_confirmation") String passwordConfirmation,
                                HttpServletResponse response) {

        ChangePasswordValidation validation = new ChangePasswordValidation(password, passwordConfirmation);
        if (!validation.valid()) {
            model.addAttribute("message_code", validation.getMessageCode());
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return "accounts/new";
        }

        AccountCreationService.AccountCreation accountCreation;
        try {
            accountCreation = accountCreationService.completeActivation(code, password);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                model.addAttribute("message_code", "email_already_taken");
            } else {
                model.addAttribute("message_code", "code_expired");
            }
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return "accounts/new";
        }

        UaaPrincipal uaaPrincipal = new UaaPrincipal(accountCreation.getUserId(), accountCreation.getUsername(), accountCreation.getUsername(), Origin.UAA, null);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(uaaPrincipal, null, UaaAuthority.USER_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(token);

        String redirectLocation = accountCreation.getRedirectLocation();
        if (redirectLocation == null) {
            redirectLocation = "home";
        }
        return "redirect:" + redirectLocation;
    }
}
