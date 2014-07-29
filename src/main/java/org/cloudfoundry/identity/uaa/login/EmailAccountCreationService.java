package org.cloudfoundry.identity.uaa.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import javax.mail.MessagingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class EmailAccountCreationService implements AccountCreationService {

    private final Log logger = LogFactory.getLog(getClass());

    private final SpringTemplateEngine templateEngine;
    private final EmailService emailService;
    private final RestTemplate uaaTemplate;
    private final String uaaBaseUrl;
    private final String brand;
    private final ObjectMapper objectMapper;

    public EmailAccountCreationService(ObjectMapper objectMapper, SpringTemplateEngine templateEngine, EmailService emailService, RestTemplate uaaTemplate, String uaaBaseUrl, String brand) {
        this.objectMapper = objectMapper;
        this.templateEngine = templateEngine;
        this.emailService = emailService;
        this.uaaTemplate = uaaTemplate;
        this.uaaBaseUrl = uaaBaseUrl;
        this.brand = brand;
    }

    @Override
    public void beginActivation(String email, String clientId) {
        String subject = getSubjectText();
        try {
            Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + (60 * 60 * 1000)); // 1 hour
            ExpiringCode expiringCodeForPost = getExpiringCode(email, clientId, expiresAt);
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
        } catch (IOException e) {
            logger.info("Exception raised while creating account activation email for " + email, e);
        }
    }

    private ExpiringCode getExpiringCode(String username, String clientId, Timestamp expiresAt) throws IOException {
        Map<String, String> codeData = new HashMap<>();
        codeData.put("username", username);
        codeData.put("client_id", clientId);
        String codeDataString = objectMapper.writeValueAsString(codeData);
        return new ExpiringCode(null, expiresAt, codeDataString);
    }

    @Override
    public AccountCreation completeActivation(String code, String password) {
        Map<String, String> accountCreationRequest = new HashMap<>();
        accountCreationRequest.put("code", code);
        accountCreationRequest.put("password", password);
        return uaaTemplate.postForObject(uaaBaseUrl + "/create_account", accountCreationRequest, AccountCreation.class);
    }

    private String getSubjectText() {
        return brand.equals("pivotal") ? "Activate your Pivotal ID" : "Activate your account";
    }

    private String getEmailHtml(String code, String email) {
        String accountsUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/accounts/new").build().toUriString();

        final Context ctx = new Context();
        ctx.setVariable("serviceName", brand.equals("pivotal") ? "Pivotal" : "Cloud Foundry");
        ctx.setVariable("servicePhrase", brand.equals("pivotal") ? "a Pivotal ID" : "an account");
        ctx.setVariable("code", code);
        ctx.setVariable("email", email);
        ctx.setVariable("accountsUrl", accountsUrl);
        return templateEngine.process("activate", ctx);
    }
}
