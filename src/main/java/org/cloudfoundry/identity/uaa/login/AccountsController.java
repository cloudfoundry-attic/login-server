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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @RequestMapping(method = POST, params = {"code", "password"})
    public String createAccount(@RequestParam("code") String code, @RequestParam("password") String password) {
        accountCreationService.completeActivation(code, password);
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
