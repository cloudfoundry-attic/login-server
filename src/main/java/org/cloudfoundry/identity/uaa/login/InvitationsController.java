package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.hibernate.validator.constraints.Email;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

import static org.cloudfoundry.identity.uaa.login.ExpiringCodeService.*;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@Controller
@RequestMapping("/invitations")
public class InvitationsController {
    private InvitationsService invitationsService;

    public InvitationsController(InvitationsService invitationsService) {
        this.invitationsService = invitationsService;
    }

    @RequestMapping(value = "/new", method = GET)
    public String newInvitePage(Model model) {
        return "invitations/new_invite";
    }
    
    @RequestMapping(value = "/accept", method = GET, params = {"code", "email"})
    public String acceptInvitePage() {
        return "invitations/accept_invite";
    }

    @RequestMapping(method = POST, params = {"email"})
    public String sendInvitationEmail(@Valid @ModelAttribute("email") ValidEmail email, BindingResult result, Model model, HttpServletResponse response) {
        if (result.hasErrors()) {
            return handleUnprocessableEntity(model, response, "invalid_email", "invitations/new_invite");
        }

        UaaPrincipal p = ((UaaPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String currentUser = p.getName();
        invitationsService.inviteUser(email.getEmail(), currentUser);
        return "invitations/invite_sent";
    }

    @RequestMapping(value = "/accept.do", method = POST, params = {"email", "code"})
    public String acceptInvitation(@RequestParam("email") String email,
                                   @RequestParam("code") String code,
                                   @RequestParam("password") String password,
                                   @RequestParam("password_confirmation") String passwordConfirmation, Model model,
                                   HttpServletResponse servletResponse) throws IOException {

        ChangePasswordValidation validation = new ChangePasswordValidation(password, passwordConfirmation);
        if (!validation.valid()) {
            return handleUnprocessableEntity(model, servletResponse, validation.getMessageCode(), "invitations/accept_invite");
        }

        InvitationsService.InvitationAcceptanceResponse response = null;
        try {
            response = invitationsService.acceptInvitation(email, password, code);
        } catch (CodeNotFoundException e) {
            return handleUnprocessableEntity(model, servletResponse, "code_expired", "invitations/accept_invite");
        }

        UaaPrincipal uaaPrincipal = new UaaPrincipal(response.getUserId(), response.getUsername(), response.getEmail(), Origin.UAA, null);
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(uaaPrincipal, null, UaaAuthority.USER_AUTHORITIES);
        SecurityContextHolder.getContext().setAuthentication(token);

        return "redirect:/home";
    }

    private String handleUnprocessableEntity(Model model, HttpServletResponse response, String errorMessage, String view) {
        model.addAttribute("error_message_code", errorMessage);
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        return view;
    }

    public static class ValidEmail {
        @Email
        String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
