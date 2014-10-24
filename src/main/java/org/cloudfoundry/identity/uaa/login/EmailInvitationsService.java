package org.cloudfoundry.identity.uaa.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import javax.mail.MessagingException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EmailInvitationsService implements InvitationsService {
    private final Log logger = LogFactory.getLog(getClass());

    private final SpringTemplateEngine templateEngine;
    private final EmailService emailService;
    private final String brand;

    public EmailInvitationsService(SpringTemplateEngine templateEngine, EmailService emailService, String brand) {
        this.templateEngine = templateEngine;
        this.emailService = emailService;
        this.brand = brand;
    }
    
    @Autowired
    private AccountCreationService accountCreationService;
    @Autowired
    private ExpiringCodeService expiringCodeService;

    private void sendInvitationEmail(String email, String currentUser, String code) {
        String subject = getSubjectText();
        try {
            String htmlContent = getEmailHtml(email, currentUser, code);
            try {
                emailService.sendMimeMessage(email, subject, htmlContent);
            } catch (MessagingException e) {
                logger.error("Exception raised while sending message to " + email, e);
            } catch (UnsupportedEncodingException e) {
                logger.error("Exception raised while sending message to " + email, e);
            }
        } catch (RestClientException e) {
            logger.info("Exception raised while creating invitation email from " + email, e);
        }
    }

    private String getSubjectText() {
        return brand.equals("pivotal") ? "Invitation to join Pivotal" : "Invitation to join Cloud Foundry";
    }

    private String getEmailHtml(String email, String currentUser, String code) {
        // TODO - queryParam is not generating the query parameters in the URL correctly - fix please!
        String accountsUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/invitations/accept").queryParam("code",code).queryParam("email",email).build().toUriString();
        final Context ctx = new Context();
        ctx.setVariable("serviceName", brand.equals("pivotal") ? "Pivotal" : "Cloud Foundry");
        ctx.setVariable("email", email);
        ctx.setVariable("currentUser", currentUser);
        ctx.setVariable("accountsUrl", accountsUrl);
        return templateEngine.process("invite", ctx);
    }

    @Override
    public void inviteUser(String email, String currentUser,String clientId) {
        // generate a code
        try {
            Map<String,String> data = new HashMap<>();
            data.put("username", email);
            data.put("client_id",clientId);
            String code = expiringCodeService.generateCode(data, 365, TimeUnit.DAYS);
            accountCreationService.createUser(email, null);
            sendInvitationEmail(email, currentUser, code);
        } catch (IOException e) {
            logger.warn("couldn't invite user",e);
        }
    }
}
