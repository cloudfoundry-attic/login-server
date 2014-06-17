package org.cloudfoundry.identity.uaa.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import javax.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;

public class EmailAccountCreationService implements AccountCreationService {

    private final Log logger = LogFactory.getLog(getClass());

    private final SpringTemplateEngine templateEngine;
    private final EmailService emailService;
    private final RestTemplate uaaTemplate;
    private final String uaaBaseUrl;
    private final String brand;

    public EmailAccountCreationService(SpringTemplateEngine templateEngine, EmailService emailService, RestTemplate uaaTemplate, String uaaBaseUrl, String brand) {
        this.templateEngine = templateEngine;
        this.emailService = emailService;
        this.uaaTemplate = uaaTemplate;
        this.uaaBaseUrl = uaaBaseUrl;
        this.brand = brand;
    }

    @Override
    public void beginActivation(String email) {
        String subject = getSubjectText();
        try {
            Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + (60 * 60 * 1000)); // 1 hour
            ExpiringCode expiringCodeForPost = new ExpiringCode(null, expiresAt, email);
            ExpiringCode expiringCode = uaaTemplate.postForObject(uaaBaseUrl + "/Codes", expiringCodeForPost, ExpiringCode.class);
            String htmlContent = getEmailHtml(expiringCode.getCode(), email);

            try {
                emailService.sendMimeMessage(email, subject, htmlContent);
            } catch (MessagingException e) {
                logger.error("Exception raised while sending message to " + email, e);
            } catch (UnsupportedEncodingException e) {
                logger.error("Exception raised while sending message to " + email, e);
            }
        } catch (RestClientException e) {
            logger.info("Exception raised while creating account activation email for " + email, e);
        }
    }

    @Override
    public String completeActivation(String code, String password) {
        return uaaTemplate.postForObject(uaaBaseUrl + "/create_account", new Account(code, password), String.class);
    }

    private String getSubjectText() {
        return brand.equals("pivotal") ? "Pivotal account activation request" : "Account activation request";
    }

    private String getEmailHtml(String code, String email) {
        String accountsUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/accounts/new").build().toUriString();

        final Context ctx = new Context();
        ctx.setVariable("servicePhrase", brand.equals("pivotal") ? "a Pivotal" : "an");
        ctx.setVariable("code", code);
        ctx.setVariable("email", email);
        ctx.setVariable("accountsUrl", accountsUrl);
        return templateEngine.process("activate", ctx);
    }

    private static class Account {
        private final String code;
        private final String password;

        public Account(String code, String password) {
            this.code = code;
            this.password = password;
        }

        public String getPassword() {
            return password;
        }

        public String getCode() {
            return code;
        }
    }
}
