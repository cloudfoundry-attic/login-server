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

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletResponse;

@Controller
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    public ResetPasswordController(ResetPasswordService resetPasswordService) {
        this.resetPasswordService = resetPasswordService;
    }

    @RequestMapping(value = "/forgot_password", method = RequestMethod.GET)
    public String forgotPasswordPage() {
        return "forgot_password";
    }

    @RequestMapping(value = "/forgot_password.do", method = RequestMethod.POST)
    public String forgotPassword(@ModelAttribute("email") String email, RedirectAttributes redirectAttributes) {
        resetPasswordService.forgotPassword(email);
        redirectAttributes.addFlashAttribute("success", Boolean.TRUE);
        return "redirect:forgot_password";
    }

    @RequestMapping(value = "/reset_password", method = RequestMethod.GET)
    public String resetPasswordPage(@RequestParam String code, Model model) {
        model.addAttribute("code", code);

        return "reset_password";
    }

    @RequestMapping(value = "/reset_password.do", method = RequestMethod.POST)
    public String resetPassword(Model model,
                                @ModelAttribute("code") String code,
                                @ModelAttribute("password") String password,
                                @ModelAttribute("password_confirmation") String passwordConfirmation,
                                HttpServletResponse response) {

        if (password.isEmpty() || passwordConfirmation.isEmpty() || !password.equals(passwordConfirmation)) {
            model.addAttribute("message", "Passwords must match and not be empty");
            response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
            return "reset_password";
        }
        resetPasswordService.resetPassword(code, password);
        return "redirect:home";
    }
}
