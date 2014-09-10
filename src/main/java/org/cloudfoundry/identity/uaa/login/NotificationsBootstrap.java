package org.cloudfoundry.identity.uaa.login;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;

public class NotificationsBootstrap implements InitializingBean {

    private List<HashMap<String, Object>> notifications;
    private final String notificationsUrl;
    private RestTemplate notificationsTemplate;
    private Environment environment;

    public Boolean getIsNotificationsRegistered() {
        return isNotificationsRegistered;
    }

    private Boolean isNotificationsRegistered = false;
    private Logger logger = Logger.getLogger(NotificationsBootstrap.class);

    public NotificationsBootstrap(String notificationsUrl, RestTemplate notificationsTemplate, Environment environment) {
        this.notificationsUrl = notificationsUrl;
        this.notificationsTemplate = notificationsTemplate;
        this.environment = environment;
    }

    public void setNotificationsTemplate(RestTemplate notificationsTemplate) {
        this.notificationsTemplate = notificationsTemplate;
    }

    public void setNotifications(List<HashMap<String, Object>> notifications) {
        this.notifications = notifications;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        registerNotifications();
    }

    public void registerNotifications()
    {
        HashMap<String, Object> request = new HashMap<>();
        request.put("source_description", "CF_Identity");
        request.put("kinds", notifications);
        try {
            if(environment.getProperty("notifications.url")!= null && environment.getProperty("notifications.url")!= "") {
                notificationsTemplate.put(notificationsUrl + "/registration", request);
                isNotificationsRegistered = true;
            }
        } catch (ResourceAccessException e) {
            logger.warn("Notifications could not be registered because notifications server is down", e);
            isNotificationsRegistered = false;
        }
    }
}
