package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.error.UaaException;
import org.cloudfoundry.identity.uaa.login.ExpiringCodeService.CodeNotFoundException;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.hibernate.validator.constraints.Email;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@Controller
@RequestMapping("/invitations")
public class InvitationsController {
    private InvitationsService invitationsService;
    @Autowired
    private ExpiringCodeService expiringCodeService;

    public InvitationsController(InvitationsService invitationsService) {
        this.invitationsService = invitationsService;
    }

    @RequestMapping(value = "/new", method = GET)
    public String newInvitePage(Model model) {
        return "invitations/new_invite";
    }
    

    @RequestMapping(value = "/new.do", method = POST, params = {"email"})
    public String sendInvitationEmail(@RequestParam String email, Model model, HttpServletResponse response) throws IOException {
        
        UaaPrincipal p = ((UaaPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        String currentUser = p.getName();
        String[] emails = email.split("[, ]");
        MultipleInvitationErrors errors = new MultipleInvitationErrors();
        for (String emailAddress : emails) {
        	if (!EmailValidator.isValid(emailAddress)) {
        		errors.invalidEmails.add(emailAddress);
        		continue;
        	}
            try {
            	invitationsService.inviteUser(emailAddress, currentUser);
            } catch (UaaException e) {
            	errors.existingEmails.add(emailAddress);
        		continue;
            }
        }
        if (!errors.existingEmails.isEmpty() || !errors.existingEmails.isEmpty()) {
        	 return handleUnprocessableEntity(model, response, "existing_user", "invitations/new_invite");
        }
        return "redirect:sent";
    }
    
    public static class MultipleInvitationErrors {
    	public List<String> invalidEmails = new ArrayList<String>();
    	public List<String> existingEmails = new ArrayList<String>();
    }
    
    @RequestMapping(value = "sent", method = GET)
    public String inviteSentPage(Model model) {
        return "invitations/invite_sent";
    }
    
    @RequestMapping(value = "/accept", method = GET, params = {"code"})
    public String acceptInvitePage(@RequestParam String code, Model model, HttpServletResponse response) throws IOException {
    	
		try {
			Map<String, String> codeData = expiringCodeService.verifyCode(code);
	        UaaPrincipal uaaPrincipal = new UaaPrincipal(codeData.get("user_id"), codeData.get("email"), codeData.get("email"), Origin.UAA, null);
	        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(uaaPrincipal, null, UaaAuthority.USER_AUTHORITIES);
	        SecurityContextHolder.getContext().setAuthentication(token);
	    	model.addAllAttributes(codeData);
	    	return "invitations/accept_invite";
        } catch (CodeNotFoundException e) {
            return handleUnprocessableEntity(model, response, "code_expired", "invitations/accept_invite");
		}
    }

    @RequestMapping(value = "/accept.do", method = POST)
    public String acceptInvitation(@RequestParam("password") String password,
                                   @RequestParam("password_confirmation") String passwordConfirmation, Model model,
                                   HttpServletResponse servletResponse) throws IOException {

        ChangePasswordValidation validation = new ChangePasswordValidation(password, passwordConfirmation);
        if (!validation.valid()) {
            return handleUnprocessableEntity(model, servletResponse, validation.getMessageCode(), "invitations/accept_invite");
        }
        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UaaPrincipal principal =  (UaaPrincipal) token.getPrincipal();
        
        invitationsService.acceptInvitation(principal.getId(), principal.getEmail(), password);

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
