package org.cloudfoundry.identity.uaa.login.feature;

import org.cloudfoundry.identity.uaa.login.test.DefaultIntegrationTestConfig;
import org.cloudfoundry.identity.uaa.login.test.IntegrationTestRule;
import org.cloudfoundry.identity.uaa.login.test.TestClient;
import org.cloudfoundry.identity.uaa.test.UaaTestAccounts;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.test.TestAccounts;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestOperations;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultIntegrationTestConfig.class)
public class PasswordGrantIT {
    @Autowired
    @Rule
    public IntegrationTestRule integrationTestRule;

    @Value("${integration.test.base_url}")
    String baseUrl;

    @Value("${integration.test.app_url}")
    String appUrl;

    @Autowired
    RestOperations restOperations;

    @Autowired
    TestClient testClient;

    @Autowired
    TestAccounts testAccounts;

    @Test
    public void testUserLoginViaPasswordGrant() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", ((UaaTestAccounts) testAccounts).getAuthorizationHeader("cf", ""));

        LinkedMultiValueMap<String, String> postBody = new LinkedMultiValueMap<>();
        postBody.add("grant_type", "password");
        postBody.add("username", testAccounts.getUserName());
        postBody.add("password", testAccounts.getPassword());

        ResponseEntity<Void> responseEntity = restOperations.exchange(baseUrl + "/oauth/token",
            HttpMethod.POST,
            new HttpEntity<>(postBody, headers),
            Void.class);

        Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }
}
