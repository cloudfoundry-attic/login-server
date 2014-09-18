package org.cloudfoundry.identity.uaa.login;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsService implements MessageService {
    private final RestTemplate notificationsTemplate;
    private final RestTemplate uaaTemplate;
    private final String notificationsUrl;
    private final Map<MessageType,HashMap<String, Object>> notifications;
    private final String uaaUrl;

    private Boolean isNotificationsRegistered = false;
    public Boolean getIsNotificationsRegistered() {
        return isNotificationsRegistered;
    }

    public NotificationsService(RestTemplate notificationsTemplate, String notificationsUrl, Map<MessageType, HashMap<String, Object>> notifications, RestTemplate uaaTemplate, String uaaUrl) {
        this.notificationsTemplate = notificationsTemplate;
        this.notificationsUrl = notificationsUrl;
        this.notifications = notifications;
        this.uaaTemplate = uaaTemplate;
        this.uaaUrl = uaaUrl;
    }

    @Override
    public void sendMessage(String email, MessageType messageType, String subject, String htmlContent) {
        if(!getIsNotificationsRegistered())
                registerNotifications();
        Map<String, Object> response = uaaTemplate.getForObject(uaaUrl + "/ids/Users?attributes=id&filter=userName eq \"" + email + "\"", Map.class);
        List<Map<String,String>> resources = (List<Map<String, String>>) response.get("resources");
        String userId = resources.get(0).get("id");

        Map<String,String> request = new HashMap<>();
        String kindId = (String)notifications.get(messageType).get("id");
        request.put("kind_id", kindId);
        request.put("subject", subject);
        request.put("text", htmlContent);
        HttpEntity<Map<String,String>> requestEntity = new HttpEntity<>(request);
        notificationsTemplate.exchange(notificationsUrl + "/users/" + userId, HttpMethod.POST, requestEntity, Void.class);
    }

    private void registerNotifications() {
        HashMap<String, Object> request = new HashMap<>();
        request.put("source_description", "CF_Identity");
        request.put("kinds", notifications.values());

        notificationsTemplate.put(notificationsUrl + "/registration", request);
        isNotificationsRegistered = true;
    }
}
