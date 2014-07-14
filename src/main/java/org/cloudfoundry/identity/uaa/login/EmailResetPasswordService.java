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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import javax.mail.MessagingException;

public class EmailResetPasswordService implements ResetPasswordService {

    private final Log logger = LogFactory.getLog(getClass());

    private final TemplateEngine templateEngine;
    private final EmailService emailService;
    private final RestTemplate uaaTemplate;
    private final String uaaBaseUrl;
    private final String brand;

    public EmailResetPasswordService(TemplateEngine templateEngine, EmailService emailService, RestTemplate uaaTemplate, String uaaBaseUrl, String brand) {
        this.templateEngine = templateEngine;
        this.emailService = emailService;
        this.uaaTemplate = uaaTemplate;
        this.uaaBaseUrl = uaaBaseUrl;
        this.brand = brand;
    }

    @Override
    public void forgotPassword(String email) {
        String subject = getSubjectText();
        String htmlContent = null;
        try {
            String code = uaaTemplate.postForObject(uaaBaseUrl + "/password_resets", email, String.class);
            htmlContent = getCodeSentEmailHtml(code, email);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                htmlContent = getResetUnavailableEmailHtml(email);
            } else {
                logger.info("Exception raised while creating password reset for " + email, e);
            }
        } catch (RestClientException e) {
            logger.error("Exception raised while creating password reset for " + email, e);
        }

        if (htmlContent != null) {
            try {
                emailService.sendMimeMessage(email, subject, htmlContent);
            } catch (MessagingException e) {
                logger.error("Exception raised while sending message to " + email, e);
            } catch (UnsupportedEncodingException e) {
                logger.error("Exception raised while sending message to " + email, e);
            }
        }
    }

    private String getSubjectText() {
        return brand.equals("pivotal") ? "Pivotal account password reset request" : "Account password reset request";
    }

    @Override
    public String resetPassword(String code, String newPassword) {
        Map<String, String> uriVariables = new HashMap<String, String>();
        uriVariables.put("baseUrl", uaaBaseUrl);

        Map<String, String> formData = new HashMap<String, String>();
        formData.put("code", code);
        formData.put("new_password", newPassword);

        return uaaTemplate.postForObject("{baseUrl}/password_change", formData, String.class, uriVariables);
    }

    private String getCodeSentEmailHtml(String code, String email) {
        String resetUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/reset_password").build().toUriString();

        final Context ctx = new Context();
        ctx.setVariable("serviceName", brand.equals("pivotal") ? "Pivotal " : "");
        ctx.setVariable("code", code);
        ctx.setVariable("email", email);
        ctx.setVariable("resetUrl", resetUrl);
        return templateEngine.process("reset_password", ctx);
    }

    private String getResetUnavailableEmailHtml(String email) {
        String hostname = ServletUriComponentsBuilder.fromCurrentContextPath().build().getHost();

        final Context ctx = new Context();
        ctx.setVariable("serviceName", brand.equals("pivotal") ? "Pivotal " : "");
        ctx.setVariable("email", email);
        ctx.setVariable("hostname", hostname);
        return templateEngine.process("reset_password_unavailable", ctx);
    }
}
