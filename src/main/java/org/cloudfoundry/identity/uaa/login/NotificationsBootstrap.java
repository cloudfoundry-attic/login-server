package org.cloudfoundry.identity.uaa.login;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;

public class NotificationsBootstrap implements InitializingBean {

    private List<HashMap<String, Object>> notifications;
    private final String notificationsUrl;
    private RestTemplate notificationsTemplate;
    private Logger logger = Logger.getLogger(NotificationsBootstrap.class);

    public NotificationsBootstrap(List<HashMap<String, Object>> notifications, String notificationsUrl, RestTemplate notificationsTemplate) {
        this.notifications = notifications;
        this.notificationsUrl = notificationsUrl;
        this.notificationsTemplate = notificationsTemplate;
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

    private void registerNotifications()
    {
        HashMap<String, Object> request = new HashMap<>();
        request.put("source_description", "CF_Identity");
        request.put("kinds", notifications);
        try {
            notificationsTemplate.put(notificationsUrl + "/registration", request);
        } catch (ResourceAccessException e) {
            logger.warn("Notifications could not be registered because notifications server is down", e);
        }
    }
}
