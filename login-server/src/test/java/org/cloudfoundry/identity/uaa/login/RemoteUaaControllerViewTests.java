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

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

import org.cloudfoundry.identity.uaa.authentication.login.Prompt;
import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
public class RemoteUaaControllerViewTests {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    RestTemplate restTemplate;

    private MockMvc mockMvc;
    private MockRestServiceServer mockRestServiceServer;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .build();

        mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void testAccessConfirmationPage() throws Exception {
        mockRestServiceServer.expect(requestTo("https://uaa.cloudfoundry.com/oauth/authorize"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(UAA_JSON, MediaType.APPLICATION_JSON));

        UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken("marissa", null, Arrays.asList(UaaAuthority.fromAuthorities("uaa.user")));

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
                .andExpect(xpath("//h1[text()='Application Authorization']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.0' and @value='scope.cloud_controller.write' and @checked='checked']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.1' and @value='scope.scim.userids' and @checked='checked']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.2' and @value='scope.password.write' and @checked='checked']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.3' and @value='scope.cloud_controller.read']").exists())
                .andExpect(xpath("//input[@type='checkbox' and @name='scope.3' and @value='scope.cloud_controller.read' and @checked='checked']").doesNotExist());
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
        RestTemplate restTemplate () {
            return new RestTemplate();
        }

        @Bean
        RemoteUaaController remoteUaaController(RestTemplate restTemplate) {
            RemoteUaaController remoteUaaController = new RemoteUaaController(new RestTemplate());
            Prompt first = new Prompt("how", "text", "How did I get here?");
            Prompt second = new Prompt("where", "password", "Where does that highway go to?");
            remoteUaaController.setPrompts(Arrays.asList(first, second));
            remoteUaaController.setAuthorizationTemplate(restTemplate);
            return remoteUaaController;
        }
    }

    private static String UAA_JSON = "{\n" +
            "    \"approved_scopes\": [{\n" +
            "            \"code\": \"scope.password.write\",\n" +
            "            \"text\": \"Access your 'password' resources with scope 'write'\"\n" +
            "        }],\n" +
            "    \"auth_request\": {\n" +
            "        \"approvalParameters\": {},\n" +
            "        \"approved\": false,\n" +
            "        \"authorities\": [\n" +
            "            {\n" +
            "                \"authority\": \"uaa.resource\"\n" +
            "            }\n" +
            "        ],\n" +
            "        \"authorizationParameters\": {\n" +
            "            \"add_new\": \"false\",\n" +
            "            \"client_id\": \"app\",\n" +
            "            \"external_scopes\": \"\",\n" +
            "            \"redirect_uri\": \"http://localhost:8080/app/\",\n" +
            "            \"response_type\": \"code\",\n" +
            "            \"scope\": \"cloud_controller.read cloud_controller.write openid password.write scim.userids\",\n" +
            "            \"source\": \"login\",\n" +
            "            \"state\": \"MSS7Nu\",\n" +
            "            \"username\": \"marissa\"\n" +
            "        },\n" +
            "        \"clientId\": \"app\",\n" +
            "        \"denied\": true,\n" +
            "        \"redirectUri\": \"http://localhost:8080/app/\",\n" +
            "        \"resourceIds\": [\n" +
            "            \"scim\",\n" +
            "            \"openid\",\n" +
            "            \"cloud_controller\",\n" +
            "            \"password\"\n" +
            "        ],\n" +
            "        \"responseTypes\": [\n" +
            "            \"code\"\n" +
            "        ],\n" +
            "        \"scope\": [\n" +
            "            \"cloud_controller.read\",\n" +
            "            \"cloud_controller.write\",\n" +
            "            \"openid\",\n" +
            "            \"password.write\",\n" +
            "            \"scim.userids\"\n" +
            "        ],\n" +
            "        \"state\": \"MSS7Nu\"\n" +
            "    },\n" +
            "    \"client\": {\n" +
            "        \"authorities\": [\n" +
            "            \"uaa.resource\"\n" +
            "        ],\n" +
            "        \"authorized_grant_types\": [\n" +
            "            \"authorization_code\",\n" +
            "            \"client_credentials\",\n" +
            "            \"implicit\",\n" +
            "            \"password\",\n" +
            "            \"refresh_token\"\n" +
            "        ],\n" +
            "        \"client_id\": \"app\",\n" +
            "        \"resource_ids\": [\n" +
            "            \"none\"\n" +
            "        ],\n" +
            "        \"scope\": [\n" +
            "            \"cloud_controller.read\",\n" +
            "            \"cloud_controller.write\",\n" +
            "            \"openid\",\n" +
            "            \"organizations.acme\",\n" +
            "            \"password.write\",\n" +
            "            \"scim.userids\"\n" +
            "        ]\n" +
            "    },\n" +
            "    \"client_id\": \"app\",\n" +
            "    \"denied_scopes\": [{\n" +
            "            \"code\": \"scope.cloud_controller.read\",\n" +
            "            \"text\": \"Access your 'cloud_controller' resources with scope 'read'\"\n" +
            "        }],\n" +
            "    \"message\": \"To confirm or deny access POST to the following locations with the parameters requested.\",\n" +
            "    \"options\": {\n" +
            "        \"confirm\": {\n" +
            "            \"key\": \"user_oauth_approval\",\n" +
            "            \"location\": \"http://localhost/uaa/oauth/authorize\",\n" +
            "            \"path\": \"/uaa/oauth/authorize\",\n" +
            "            \"value\": \"true\"\n" +
            "        },\n" +
            "        \"deny\": {\n" +
            "            \"key\": \"user_oauth_approval\",\n" +
            "            \"location\": \"http://localhost/uaa/oauth/authorize\",\n" +
            "            \"path\": \"/uaa/oauth/authorize\",\n" +
            "            \"value\": \"false\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"redirect_uri\": \"http://localhost:8080/app/\",\n" +
            "    \"scopes\": [\n" +
            "        {\n" +
            "            \"code\": \"scope.password.write\",\n" +
            "            \"text\": \"Access your 'password' resources with scope 'write'\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"code\": \"scope.cloud_controller.read\",\n" +
            "            \"text\": \"Access your 'cloud_controller' resources with scope 'read'\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"code\": \"scope.cloud_controller.write\",\n" +
            "            \"text\": \"Access your 'cloud_controller' resources with scope 'write'\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"code\": \"scope.scim.userids\",\n" +
            "            \"text\": \"Access your 'scim' resources with scope 'userids'\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"undecided_scopes\": [\n" +
            "        {\n" +
            "            \"code\": \"scope.cloud_controller.write\",\n" +
            "            \"text\": \"Access your 'cloud_controller' resources with scope 'write'\"\n" +
            "        },\n" +
            "        {\n" +
            "            \"code\": \"scope.scim.userids\",\n" +
            "            \"text\": \"Access your 'scim' resources with scope 'userids'\"\n" +
            "        }\n" +
            "    ]\n" +
            "}\n";
}
