package org.cloudfoundry.identity.uaa.login;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@Controller
@RequestMapping("/invitations")
public class InvitationsController {
    public InvitationsService getInvitationsService() {
        return invitationsService;
    }

    public void setInvitationsService(InvitationsService invitationsService) {
        this.invitationsService = invitationsService;
    }

    private InvitationsService invitationsService;

    public InvitationsController(InvitationsService invitationsService) {
        this.invitationsService = invitationsService;
    }

    @RequestMapping(value = "/new", method = GET)
    public String newInvitePage(Model model) {
        return "new_invite";
    }

    @RequestMapping(method = POST, params = {"email"})
    public String sendInvitationEmail(@RequestParam("email") String email,
                                      Principal principal) {
        String currentUser = principal.getName();
        invitationsService.sendInviteEmail(email, currentUser);
        return "invite_sent";
    }
}
