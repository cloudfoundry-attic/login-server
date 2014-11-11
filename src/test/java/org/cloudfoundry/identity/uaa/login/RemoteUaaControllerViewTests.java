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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import org.cloudfoundry.identity.uaa.authentication.login.Prompt;
import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.cloudfoundry.identity.uaa.test.UaaTestAccounts;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = RemoteUaaControllerViewTests.ContextConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RemoteUaaControllerViewTests {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    MockEnvironment environment;

    private MockMvc mockMvc;
    private MockRestServiceServer mockRestServiceServer;
    
    private UaaTestAccounts testAccounts = UaaTestAccounts.standard(null);

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();

        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testDefaultBranding() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(xpath("//head/link[@rel='shortcut icon']/@href").string("/resources/oss/images/favicon.ico"))
                .andExpect(xpath("//head/link[@href='/resources/oss/stylesheets/application.css']").exists())
                .andExpect(xpath("//div[@class='header' and contains(@style,'/resources/oss/images/logo.png')]").exists());
    }

    @Test
    public void testExternalizedBranding() throws Exception {
        environment.setProperty("assetBaseUrl", "//cdn.example.com/pivotal");

        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
                .andExpect(xpath("//head/link[@rel='shortcut icon']/@href").string("//cdn.example.com/pivotal/images/favicon.ico"))
                .andExpect(xpath("//head/link[@href='//cdn.example.com/pivotal/stylesheets/application.css']").exists())
                .andExpect(xpath("//div[@class='header' and contains(@style,'//cdn.example.com/pivotal/images/logo.png')]").exists());
    }

    @Test
    public void testAccessConfirmationPage() throws Exception {
        mockRestServiceServer.expect(requestTo("https://uaa.cloudfoundry.com/oauth/authorize"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(UAA_JSON, MediaType.APPLICATION_JSON));

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(testAccounts.getUserName(), null, Arrays.asList(UaaAuthority.fromAuthorities("uaa.user")));

        MockHttpServletRequestBuilder get = get("/oauth/authorize")
                .param("response_type", "code")
                .param("client_id", "app")
                .param("redirect_uri", "http://example.com")
                .principal(principal);

        mockMvc.perform(get)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("client_id"))
                .andExpect(model().attribute("undecided_scopes", hasSize(2)))
                .andExpect(model().attribute("approved_scopes", hasSize(1)))
                .andExpect(model().attribute("denied_scopes", hasSize(1)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(xpath("//h1[text()='Application Authorization']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.0' and @value='scope.cloud_controller.write' and @checked='checked']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.1' and @value='scope.scim.userids' and @checked='checked']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.2' and @value='scope.password.write' and @checked='checked']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.3' and @value='scope.cloud_controller.read']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.3' and @value='scope.cloud_controller.read' and @checked='checked']").doesNotExist());
    }

    @Test
    public void testSignupsAndResetPasswordEnabled() throws Exception {
        environment.setProperty("login.selfServiceLinksEnabled", "true");

        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
            .andExpect(xpath("//a[text()='Create account']").exists())
            .andExpect(xpath("//a[text()='Reset password']").exists());
    }

    @Test
    public void testSignupsAndResetPasswordDisabledWithNoLinksConfigured() throws Exception {
        environment.setProperty("login.selfServiceLinksEnabled", "false");

        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
            .andExpect(xpath("//a[text()='Create account']").doesNotExist())
            .andExpect(xpath("//a[text()='Reset password']").doesNotExist());
    }

    @Test
    public void testSignupsAndResetPasswordDisabledWithSomeLinksConfigured() throws Exception {
        environment.setProperty("login.selfServiceLinksEnabled", "false");
        environment.setProperty("links.signup", "http://example.com/signup");
        environment.setProperty("links.passwd", "http://example.com/reset_passwd");

        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
            .andExpect(xpath("//a[text()='Create account']").doesNotExist())
            .andExpect(xpath("//a[text()='Reset password']").doesNotExist());
    }
    
    @Test
    public void testSignupsAndResetPasswordEnabledWithCustomLinks() throws Exception {
        environment.setProperty("login.selfServiceLinksEnabled", "true");
        environment.setProperty("links.signup", "http://example.com/signup");
        environment.setProperty("links.passwd", "http://example.com/reset_passwd");

        mockMvc.perform(MockMvcRequestBuilders.get("/login"))
            .andExpect(xpath("//a[text()='Create account']/@href").string("http://example.com/signup"))
            .andExpect(xpath("//a[text()='Reset password']/@href").string("http://example.com/reset_passwd"));
    }

    @Configuration
    @EnableWebMvc
    @Import(ThymeleafConfig.class)
    static class ContextConfiguration extends WebMvcConfigurerAdapter {

        @Override
        public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
            configurer.enable();
        }

        @Bean
        BuildInfo buildInfo() {
            return new BuildInfo();
        }

        @Bean
        RestTemplate restTemplate() {
            return new RestTemplate();
        }

        @Bean
        MockEnvironment environment() {
            return new MockEnvironment();
        }

        @Bean
        RemoteUaaController remoteUaaController(MockEnvironment environment, RestTemplate restTemplate) {
            RemoteUaaController remoteUaaController = new RemoteUaaController(environment, new RestTemplate());
            Prompt first = new Prompt("how", "text", "How did I get here?");
            Prompt second = new Prompt("where", "password", "Where does that highway go to?");
            remoteUaaController.setPrompts(Arrays.asList(first, second));
            remoteUaaController.setAuthorizationTemplate(restTemplate);
            return remoteUaaController;
        }
    }

    private static String UAA_JSON = "{" +
            "    \"approved_scopes\": [{" +
            "            \"code\": \"scope.password.write\"," +
            "            \"text\": \"Access your 'password' resources with scope 'write'\"" +
            "        }]," +
            "    \"auth_request\": {" +
            "        \"approvalParameters\": {}," +
            "        \"approved\": false," +
            "        \"authorities\": [" +
            "            {" +
            "                \"authority\": \"uaa.resource\"" +
            "            }" +
            "        ]," +
            "        \"authorizationParameters\": {" +
            "            \"add_new\": \"false\"," +
            "            \"client_id\": \"app\"," +
            "            \"external_scopes\": \"\"," +
            "            \"redirect_uri\": \"http://localhost:8080/app/\"," +
            "            \"response_type\": \"code\"," +
            "            \"scope\": \"cloud_controller.read cloud_controller.write openid password.write scim.userids\"," +
            "            \"source\": \"login\"," +
            "            \"state\": \"MSS7Nu\"," +
            "            \"username\": \"marissa\"" +
            "        }," +
            "        \"clientId\": \"app\"," +
            "        \"denied\": true," +
            "        \"redirectUri\": \"http://localhost:8080/app/\"," +
            "        \"resourceIds\": [" +
            "            \"scim\"," +
            "            \"openid\"," +
            "            \"cloud_controller\"," +
            "            \"password\"" +
            "        ]," +
            "        \"responseTypes\": [" +
            "            \"code\"" +
            "        ]," +
            "        \"scope\": [" +
            "            \"cloud_controller.read\"," +
            "            \"cloud_controller.write\"," +
            "            \"openid\"," +
            "            \"password.write\"," +
            "            \"scim.userids\"" +
            "        ]," +
            "        \"state\": \"MSS7Nu\"" +
            "    }," +
            "    \"client\": {" +
            "        \"authorities\": [" +
            "            \"uaa.resource\"" +
            "        ]," +
            "        \"authorized_grant_types\": [" +
            "            \"authorization_code\"," +
            "            \"client_credentials\"," +
            "            \"implicit\"," +
            "            \"password\"," +
            "            \"refresh_token\"" +
            "        ]," +
            "        \"client_id\": \"app\"," +
            "        \"resource_ids\": [" +
            "            \"none\"" +
            "        ]," +
            "        \"scope\": [" +
            "            \"cloud_controller.read\"," +
            "            \"cloud_controller.write\"," +
            "            \"openid\"," +
            "            \"organizations.acme\"," +
            "            \"password.write\"," +
            "            \"scim.userids\"" +
            "        ]" +
            "    }," +
            "    \"client_id\": \"app\"," +
            "    \"denied_scopes\": [{" +
            "            \"code\": \"scope.cloud_controller.read\"," +
            "            \"text\": \"Access your 'cloud_controller' resources with scope 'read'\"" +
            "        }]," +
            "    \"message\": \"To confirm or deny access POST to the following locations with the parameters requested.\"," +
            "    \"options\": {" +
            "        \"confirm\": {" +
            "            \"key\": \"user_oauth_approval\"," +
            "            \"location\": \"http://localhost/uaa/oauth/authorize\"," +
            "            \"path\": \"/uaa/oauth/authorize\"," +
            "            \"value\": \"true\"" +
            "        }," +
            "        \"deny\": {" +
            "            \"key\": \"user_oauth_approval\"," +
            "            \"location\": \"http://localhost/uaa/oauth/authorize\"," +
            "            \"path\": \"/uaa/oauth/authorize\"," +
            "            \"value\": \"false\"" +
            "        }" +
            "    }," +
            "    \"redirect_uri\": \"http://localhost:8080/app/\"," +
            "    \"scopes\": [" +
            "        {" +
            "            \"code\": \"scope.password.write\"," +
            "            \"text\": \"Access your 'password' resources with scope 'write'\"" +
            "        }," +
            "        {" +
            "            \"code\": \"scope.cloud_controller.read\"," +
            "            \"text\": \"Access your 'cloud_controller' resources with scope 'read'\"" +
            "        }," +
            "        {" +
            "            \"code\": \"scope.cloud_controller.write\"," +
            "            \"text\": \"Access your 'cloud_controller' resources with scope 'write'\"" +
            "        }," +
            "        {" +
            "            \"code\": \"scope.scim.userids\"," +
            "            \"text\": \"Access your 'scim' resources with scope 'userids'\"" +
            "        }" +
            "    ]," +
            "    \"undecided_scopes\": [" +
            "        {" +
            "            \"code\": \"scope.cloud_controller.write\"," +
            "            \"text\": \"Access your 'cloud_controller' resources with scope 'write'\"" +
            "        }," +
            "        {" +
            "            \"code\": \"scope.scim.userids\"," +
            "            \"text\": \"Access your 'scim' resources with scope 'userids'\"" +
            "        }" +
            "    ]" +
            "}";
}
