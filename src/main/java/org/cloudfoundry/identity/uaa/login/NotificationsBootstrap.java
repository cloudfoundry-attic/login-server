package org.cloudfoundry.identity.uaa.login;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsBootstrap implements InitializingBean {

    private final Map<MessageType, HashMap<String, Object>> notifications;
    private final String notificationsUrl;
    private final RestTemplate notificationsTemplate;
    private Environment environment;

    public Boolean getIsNotificationsRegistered() {
        return isNotificationsRegistered;
    }

    private Boolean isNotificationsRegistered = false;
    private Logger logger = Logger.getLogger(NotificationsBootstrap.class);

    public NotificationsBootstrap(Map<MessageType, HashMap<String, Object>> notifications, String notificationsUrl, RestTemplate notificationsTemplate, Environment environment) {
        this.notificationsUrl = notificationsUrl;
        this.notificationsTemplate = notificationsTemplate;
        this.environment = environment;
        this.notifications = notifications;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            registerNotifications();
        } catch (ResourceAccessException e) {
            logger.warn("Notifications could not be registered because notifications server is down", e);
            isNotificationsRegistered = false;
        }
    }

    public void registerNotifications() {
        HashMap<String, Object> request = new HashMap<>();
        request.put("source_description", "CF_Identity");
        request.put("kinds", notifications.values());

        if (environment.getProperty("notifications.url") != null && environment.getProperty("notifications.url") != "") {
            notificationsTemplate.put(notificationsUrl + "/registration", request);
            isNotificationsRegistered = true;
        }
    }
}
