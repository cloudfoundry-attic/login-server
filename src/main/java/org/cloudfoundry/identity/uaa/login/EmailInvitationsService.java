package org.cloudfoundry.identity.uaa.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;

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

    @Override
    public void sendInviteEmail(String email, String currentUser) {
        String subject = getSubjectText();
        try {
            String htmlContent = getEmailHtml(email, currentUser);
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

    private String getEmailHtml(String email, String currentUser) {
        String accountsUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/accounts/new").build().toUriString();

        final Context ctx = new Context();
        ctx.setVariable("serviceName", brand.equals("pivotal") ? "Pivotal" : "Cloud Foundry");
        ctx.setVariable("email", email);
        ctx.setVariable("currentUser", currentUser);
        ctx.setVariable("accountsUrl", accountsUrl);
        return templateEngine.process("invite", ctx);
    }
}
