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

import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;

import javax.servlet.http.HttpServletResponse;

import java.util.Arrays;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@RequestMapping("/accounts")
public class AccountsController {

    private final AccountCreationService accountCreationService;

    public AccountsController(AccountCreationService accountCreationService) {
        this.accountCreationService = accountCreationService;
    }

    @RequestMapping(method = POST, params = "email")
    public String sendActivationEmail(@RequestParam("email") String email) {
        accountCreationService.beginActivation(email);
        return "redirect:email_sent?code=activation";
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

        String username;
        try {
            username = accountCreationService.completeActivation(code, password);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                model.addAttribute("message_code", "email_already_taken");
            } else {
                model.addAttribute("message_code", "code_expired");
            }
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return "accounts/new";
        }

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, null, Arrays.asList(UaaAuthority.UAA_USER));
        SecurityContextHolder.getContext().setAuthentication(token);

        return "redirect:home";
    }

    @RequestMapping(value = "/new", method = GET)
    public String activationEmail() {
        return "accounts/new_activation_email";
    }

    @RequestMapping(value = "/new", method = GET, params = {"code", "email"})
    public String newAccount() {
        return "accounts/new";
    }
}
