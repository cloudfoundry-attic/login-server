package org.cloudfoundry.identity.uaa.login;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;

public class NotificationsServiceTest {

    private RestTemplate notificationsTemplate;
    private RestTemplate uaaTemplate;
    private NotificationsBootstrap notificationsBootstrap;
    private Map<MessageType, HashMap<String, Object>> notifications;
    private NotificationsService notificationsService;
    private Map<String, Object> response;

    @Before
    public void setUp(){
        notificationsTemplate = Mockito.mock(RestTemplate.class);
        uaaTemplate = Mockito.mock(RestTemplate.class);
        notificationsBootstrap = Mockito.mock(NotificationsBootstrap.class);
        notifications = new HashMap<>();
        HashMap<String, Object> passwordResetNotification = new HashMap<>();

        passwordResetNotification.put("id", "kind-id-01");
        passwordResetNotification.put("description", "password reset");
        notifications.put(MessageType.PASSWORD_RESET, passwordResetNotification);

        notificationsService = new NotificationsService(notificationsTemplate, "http://example.com", notificationsBootstrap, notifications, uaaTemplate, "http://uaa.com");

        response = new HashMap<>();
        List<Map<String, String>> resources = new ArrayList<>();
        Map<String,String> userDetails = new HashMap<>();
        userDetails.put("id", "user-id-01");
        resources.add(userDetails);
        response.put("resources", resources);
    }

    @Test
    public void testSendMessage() throws Exception {
        when(uaaTemplate.getForObject("http://uaa.com/ids/Users?attributes=id&filter=userName eq \"user@example.com\"", Map.class)).thenReturn(response);

        when(notificationsBootstrap.getIsNotificationsRegistered()).thenReturn(true);
        notificationsService.sendMessage("user@example.com", MessageType.PASSWORD_RESET, "Subject", "<p>Text</p>");

        verify(notificationsBootstrap).getIsNotificationsRegistered();
        verifyNoMoreInteractions(notificationsBootstrap);

        ArgumentCaptor<HttpEntity<Map<String,String>>> requestArgument = ArgumentCaptor.forClass((Class) HttpEntity.class);
        verify(notificationsTemplate).exchange(eq("http://example.com/users/user-id-01"), eq(POST), requestArgument.capture(), eq(Void.class));
        HttpEntity<Map<String, String>> httpRequest = requestArgument.getValue();
        Map<String,String> request = httpRequest.getBody();
        assertThat(request.values(), containsInAnyOrder("kind-id-01", "Subject", "<p>Text</p>"));
    }

    @Test
    public void testSendMessageWhenNotificationsNotRegistered() throws Exception {
        when(uaaTemplate.getForObject("http://uaa.com/ids/Users?attributes=id&filter=userName eq \"user@example.com\"", Map.class)).thenReturn(response);
        when(notificationsBootstrap.getIsNotificationsRegistered()).thenReturn(false);

        notificationsService.sendMessage("user@example.com", MessageType.PASSWORD_RESET, "Subject", "<p>Text</p>");
        verify(notificationsBootstrap).registerNotifications();
    }


}
