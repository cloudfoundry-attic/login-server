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

import org.cloudfoundry.identity.uaa.config.YamlPropertiesFactoryBean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ViewResolver;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Dave Syer
 * 
 */
public class BootstrapTests {

    private GenericXmlApplicationContext context;

    @Before
    public void setup() throws Exception {
        System.clearProperty("spring.profiles.active");
    }

    @After
    public void cleanup() throws Exception {
        System.clearProperty("spring.profiles.active");
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testRootContextDefaults() throws Exception {
        context = getServletContext(null, "./src/test/resources/test/config/login.yml", "file:./src/main/webapp/WEB-INF/spring-servlet.xml");
        assertNotNull(context.getBean("viewResolver", ViewResolver.class));
        assertNotNull(context.getBean("resetPasswordController", ResetPasswordController.class));
    }

    @Test
    public void testSamlProfile() throws Exception {
        System.setProperty("idpMetadataFile", "./src/test/resources/test.saml.metadata");
        context = getServletContext("saml,fileMetadata", "./src/main/resources/login.yml", "file:./src/main/webapp/WEB-INF/spring-servlet.xml");
        assertNotNull(context.getBean("viewResolver", ViewResolver.class));
        assertNotNull(context.getBean("samlLogger", SAMLDefaultLogger.class));
    }

    private GenericXmlApplicationContext getServletContext(String profiles, String loginYmlPath, String... resources) {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext();

        if (profiles != null) {
            context.getEnvironment().setActiveProfiles(StringUtils.commaDelimitedListToStringArray(profiles));
        }

        context.load(resources);

        // Simulate what happens in the webapp when the
        // YamlServletProfileInitializer kicks in
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new Resource[] { new FileSystemResource(loginYmlPath) });
        context.getEnvironment().getPropertySources()
                        .addLast(new PropertiesPropertySource("servletProperties", factory.getObject()));

        context.refresh();

        return context;
    }
}
