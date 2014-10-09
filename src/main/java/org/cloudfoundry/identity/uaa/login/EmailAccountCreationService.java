package org.cloudfoundry.identity.uaa.login;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.codestore.ExpiringCode;
import org.cloudfoundry.identity.uaa.scim.ScimUser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class EmailAccountCreationService implements AccountCreationService {

    public static final String SIGNUP_REDIRECT_URL = "signup_redirect_url";

    private final Log logger = LogFactory.getLog(getClass());

    private final SpringTemplateEngine templateEngine;
    private final MessageService messageService;
    private final RestTemplate uaaTemplate;
    private final String uaaBaseUrl;
    private final String brand;
    private final ObjectMapper objectMapper;

    public EmailAccountCreationService(ObjectMapper objectMapper, SpringTemplateEngine templateEngine, MessageService messageService, RestTemplate uaaTemplate, String uaaBaseUrl, String brand) {
        this.objectMapper = objectMapper;
        this.templateEngine = templateEngine;
        this.messageService = messageService;
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

            messageService.sendMessage(null, email, MessageType.CREATE_ACCOUNT_CONFIRMATION, subject, htmlContent);
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
    public AccountCreation completeActivation(String code, String password) throws IOException{

        ExpiringCode expiringCode = uaaTemplate.getForObject(uaaBaseUrl + "/Codes/"+ code, ExpiringCode.class);
        Map<String, String> data = objectMapper.readValue(expiringCode.getData(), new TypeReference<Map<String, String>>() {});

        ScimUser scimUser = new ScimUser();
        scimUser.setUserName(data.get("username"));
        ScimUser.Email email = new ScimUser.Email();
        email.setPrimary(true);
        email.setValue(data.get("username"));
        scimUser.setEmails(Arrays.asList(email));
        scimUser.setOrigin(Origin.UAA);
        scimUser.setPassword(password);
        ScimUser userResponse = uaaTemplate.postForObject(uaaBaseUrl + "/Users", scimUser, ScimUser.class);


        ClientDetails clientDetails = uaaTemplate.getForObject(uaaBaseUrl + "/oauth/clients/" + data.get("client_id"), BaseClientDetails.class);
        String redirectLocation = (String) clientDetails.getAdditionalInformation().get(SIGNUP_REDIRECT_URL);

        AccountCreation accountCreation = new AccountCreation();
        accountCreation.setUserId(userResponse.getId());
        accountCreation.setUsername(userResponse.getUserName());
        accountCreation.setRedirectLocation(redirectLocation);
        return accountCreation;
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
