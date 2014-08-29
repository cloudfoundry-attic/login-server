package org.cloudfoundry.identity.uaa.login;

import junit.framework.Assert;
import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.config.YamlServletProfileInitializer;
import org.cloudfoundry.identity.uaa.login.test.TestClient;

import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mockito.Mock;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.XmlWebApplicationContext;


import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

public class InvitationsControllerTest {

    private static XmlWebApplicationContext webApplicationContext;
    private static MockMvc mockMvc;

    @BeforeClass
    public static void setUp() throws Exception {
        webApplicationContext = new XmlWebApplicationContext();
        webApplicationContext.setServletContext(new MockServletContext());
        webApplicationContext.setEnvironment(new MockEnvironment());
        ((MockEnvironment) webApplicationContext.getEnvironment()).setProperty("login.invitationsEnabled", "true");
        new YamlServletProfileInitializer().initialize(webApplicationContext);
        webApplicationContext.setConfigLocation("file:./src/main/webapp/WEB-INF/spring-servlet.xml");
        webApplicationContext.refresh();
        FilterChainProxy springSecurityFilterChain = webApplicationContext.getBean(FilterChainProxy.class);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).addFilter(springSecurityFilterChain)
            .build();

        InvitationsController controller = webApplicationContext.getBean(InvitationsController.class);
        InvitationsService service = mock(InvitationsService.class);
        controller.setInvitationsService(service);
    }

    @Test
    public void testNewInvitePage() throws Exception {
        UaaPrincipal p = new UaaPrincipal("123","marissa","marissa@test.org", Origin.UAA,"");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(p, "", UaaAuthority.USER_AUTHORITIES);
        Assert.assertTrue(auth.isAuthenticated());
        SecurityContextHolder.getContext().setAuthentication(auth);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new MockSecurityContext(auth)
        );

        MockHttpServletRequestBuilder get = get("/invitations/new")
            .session(session);

        mockMvc.perform(get)
            .andExpect(status().isOk())
            .andExpect(view().name("new_invite"));
    }

    @Test
    public void testSendInvitationEmail() throws Exception {
        UaaPrincipal p = new UaaPrincipal("123","marissa","marissa@test.org", Origin.UAA,"");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(p, "", UaaAuthority.USER_AUTHORITIES);
        Assert.assertTrue(auth.isAuthenticated());
        SecurityContextHolder.getContext().setAuthentication(auth);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            new MockSecurityContext(auth)
        );

        MockHttpServletRequestBuilder post = post("/invitations")
            .session(session)
            .param("email", "user1@example.com");

        mockMvc.perform(post)
            .andExpect(status().isOk())
            .andExpect(view().name("invite_sent"));
    }

    public static class MockSecurityContext implements SecurityContext {

        private static final long serialVersionUID = -1386535243513362694L;

        private Authentication authentication;

        public MockSecurityContext(Authentication authentication) {
            this.authentication = authentication;
        }

        @Override
        public Authentication getAuthentication() {
            return this.authentication;
        }

        @Override
        public void setAuthentication(Authentication authentication) {
            this.authentication = authentication;
        }
    }

}
