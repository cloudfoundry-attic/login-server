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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class EmailResetPasswordService implements ResetPasswordService {

    private final Log logger = LogFactory.getLog(getClass());

    private final RestTemplate uaaTemplate;
    private final String uaaBaseUrl;
    private final JavaMailSender mailSender;
    private final String brand;

    public EmailResetPasswordService(RestTemplate uaaTemplate, String uaaBaseUrl, JavaMailSender mailSender, String brand) {
        this.uaaTemplate = uaaTemplate;
        this.uaaBaseUrl = uaaBaseUrl;
        this.mailSender = mailSender;
        this.brand = brand;
    }

    @Override
    public void forgotPassword(UriComponentsBuilder uriComponentsBuilder, String email) {
        try {
            String code = uaaTemplate.postForObject(uaaBaseUrl + "/password_resets", email, String.class);
            try {
                MimeMessage message = mailSender.createMimeMessage();
                message.addRecipients(Message.RecipientType.TO, email);
                message.setSubject(getSubjectText());
                message.setContent(getEmailHtml(uriComponentsBuilder, code, email), "text/html");
                mailSender.send(message);
            } catch (MessagingException e) {
                logger.error("Exception raised while sending message to " + email, e);
            }
        } catch (RestClientException e) {
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

    private String getEmailHtml(UriComponentsBuilder uriComponentsBuilder, String code, String email) {
        String resetUrl = uriComponentsBuilder
                .path("/reset_password")
                .queryParam("code", code)
                .queryParam("email", email)
                .build().toUriString();

        String serviceName = brand.equals("pivotal") ? "Pivotal " : "";

        return "<html>" +
                "<head></head>" +
                "<body>" +
                "A request has been made to reset your " + serviceName + "account password for " + email + ".<br />" +
                "<br />" +
                "<a href=\"" + resetUrl + "\">Reset your password</a><br />" +
                "<br />" +
                "If you did not make this request, simply ignore this message and your old password will continue to work." +
                "</body>" +
                "</html>";
    }
}
