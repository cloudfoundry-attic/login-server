package org.cloudfoundry.identity.uaa.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpMethod.POST;

public class EmailChangeEmailService implements ChangeEmailService {
    private final Log logger = LogFactory.getLog(getClass());

    private final TemplateEngine templateEngine;
    private final MessageService messageService;
    private final RestTemplate uaaTemplate;
    private final String uaaBaseUrl;
    private final String brand;
    private final ObjectMapper objectMapper;

    public EmailChangeEmailService(TemplateEngine templateEngine, MessageService messageService, RestTemplate uaaTemplate, String uaaBaseUrl, String brand, ObjectMapper objectMapper) {
        this.templateEngine = templateEngine;
        this.messageService = messageService;
        this.uaaTemplate = uaaTemplate;
        this.uaaBaseUrl = uaaBaseUrl;
        this.brand = brand;
        this.objectMapper = objectMapper;
    }

    @Override
    public void beginEmailChange(String userId, String email, String newEmail) {
        ExpiringCode expiringCodeForPost = null;
        try {
            expiringCodeForPost = getExpiringCode(userId, newEmail);
        } catch (IOException e) {
            logger.info("Exception raised while creating account activation email for " + newEmail, e);
        }
        ExpiringCode expiringCode = uaaTemplate.postForObject(uaaBaseUrl + "/Codes", expiringCodeForPost, ExpiringCode.class);
        String subject = getSubjectText();
        String htmlContent = getEmailChangeEmailHtml(email, newEmail, expiringCode.getCode());

        if(htmlContent != null) {
            messageService.sendMessage(newEmail, MessageType.CHANGE_EMAIL, subject, htmlContent);
        }
    }

    @Override
    public Map<String, String> completeVerification(String code) {
        ResponseEntity<Map<String, String>> responseEntity = uaaTemplate.exchange(uaaBaseUrl + "/email_changes", POST, new HttpEntity<>(code), new ParameterizedTypeReference<Map<String, String>>() {
            });
        return responseEntity.getBody();
    }

    private ExpiringCode getExpiringCode(String userId, String email) throws IOException {
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + (60 * 60 * 1000));
        Map<String, String> codeData = new HashMap<>();
        codeData.put("userId", userId);
        codeData.put("email", email);
        String codeDataString = objectMapper.writeValueAsString(codeData);
        return new ExpiringCode(null, expiresAt, codeDataString);
    }

    private String getSubjectText() {
        return "Email change verification";
    }

    private String getEmailChangeEmailHtml(String email, String newEmail, String code) {
        String verifyUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/verify_email").build().toUriString();

        final Context ctx = new Context();
        ctx.setVariable("serviceName", brand.equals("pivotal") ? "Pivotal " : "");
        ctx.setVariable("code", code);
        ctx.setVariable("newEmail", newEmail);
        ctx.setVariable("email", email);
        ctx.setVariable("verifyUrl", verifyUrl);
        return templateEngine.process("verify_email", ctx);
    }

}
