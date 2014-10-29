package org.cloudfoundry.identity.uaa.login;

import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;

import org.cloudfoundry.identity.uaa.login.test.ThymeleafConfig;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static junit.framework.Assert.*;
import static org.cloudfoundry.identity.uaa.login.ExpiringCodeService.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = InvitationsControllerTest.ContextConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class InvitationsControllerTest {

    private MockMvc mockMvc;

    @Autowired
    ConfigurableWebApplicationContext webApplicationContext;

    @Autowired
    InvitationsService invitationsService;

    @Before
    public void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .build();
    }

    @Test
    public void testNewInvitePage() throws Exception {
        MockHttpServletRequestBuilder get = get("/invitations/new");

        mockMvc.perform(get)
            .andExpect(status().isOk())
            .andExpect(view().name("invitations/new_invite"));
    }

    @Test
    public void testSendInvitationEmail() throws Exception {
        UaaPrincipal p = new UaaPrincipal("123","marissa","marissa@test.org", Origin.UAA,"");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(p, "", UaaAuthority.USER_AUTHORITIES);
        assertTrue(auth.isAuthenticated());
        MockSecurityContext mockSecurityContext = new MockSecurityContext(auth);
        SecurityContextHolder.setContext(mockSecurityContext);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            mockSecurityContext
        );

        MockHttpServletRequestBuilder post = post("/invitations")
            .param("email", "user1@example.com");

        mockMvc.perform(post)
            .andExpect(status().isOk())
            .andExpect(view().name("invitations/invite_sent"));
        verify(invitationsService).inviteUser("user1@example.com", "marissa");
    }

    @Test
    public void testSendInvitationWithInvalidEmail() throws Exception {
        UaaPrincipal p = new UaaPrincipal("123","marissa","marissa@test.org", Origin.UAA,"");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(p, "", UaaAuthority.USER_AUTHORITIES);
        assertTrue(auth.isAuthenticated());
        MockSecurityContext mockSecurityContext = new MockSecurityContext(auth);
        SecurityContextHolder.setContext(mockSecurityContext);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
            mockSecurityContext
        );

        MockHttpServletRequestBuilder post = post("/invitations")
            .param("email", "not_a_real_email");

        mockMvc.perform(post)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(model().attribute("error_message_code", "invalid_email"))
            .andExpect(view().name("invitations/new_invite"));

        verifyZeroInteractions(invitationsService);
    }

    @Test
    public void testAcceptInvitationsPage() throws Exception {
        MockHttpServletRequestBuilder get = get("/invitations/accept")
                                            .param("code", "the_secret_code")
                                            .param("email", "user@example.com");

        mockMvc.perform(get)
            .andExpect(status().isOk())
            .andExpect(view().name("invitations/accept_invite"));
    }

    @Test
    public void testAcceptInvite() throws Exception {
        when(invitationsService.acceptInvitation("user@example.com", "password", "the_secret_code"))
            .thenReturn(new InvitationsService.InvitationAcceptanceResponse("user-id-001", "user@example.com", "user@example.com"));

        MockHttpServletRequestBuilder post = post("/invitations/accept.do")
            .param("email", "user@example.com")
            .param("password", "password")
            .param("password_confirmation", "password")
            .param("code", "the_secret_code");

        mockMvc.perform(post)
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/home"));
        verify(invitationsService).acceptInvitation("user@example.com", "password", "the_secret_code");

        UaaPrincipal principal = ((UaaPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals("user-id-001", principal.getId());
        assertEquals("user@example.com", principal.getName());
        assertEquals("user@example.com", principal.getEmail());
    }

    @Test
    public void testAcceptInviteWithExpiredCode() throws Exception {
        when(invitationsService.acceptInvitation("user@example.com", "password", "the_secret_code"))
            .thenThrow(new CodeNotFoundException("code expired"));

        MockHttpServletRequestBuilder post = post("/invitations/accept.do")
            .param("email", "user@example.com")
            .param("password", "password")
            .param("password_confirmation", "password")
            .param("code", "the_secret_code");

        mockMvc.perform(post)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(model().attribute("error_message_code", "code_expired"))
            .andExpect(view().name("invitations/accept_invite"));
    }

    @Test
    public void testAcceptInviteWithoutMatchingPasswords() throws Exception {

        MockHttpServletRequestBuilder post = post("/invitations/accept.do")
            .param("email", "user@example.com")
            .param("password", "password")
            .param("password_confirmation", "does not match")
            .param("code", "the_secret_code");

        mockMvc.perform(post)
            .andExpect(status().isUnprocessableEntity())
            .andExpect(model().attribute("error_message_code", "form_error"))
            .andExpect(view().name("invitations/accept_invite"));

        verifyZeroInteractions(invitationsService);
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
        public ResourceBundleMessageSource messageSource() {
            ResourceBundleMessageSource resourceBundleMessageSource = new ResourceBundleMessageSource();
            resourceBundleMessageSource.setBasename("messages");
            return resourceBundleMessageSource;
        }

        @Bean
        InvitationsService invitationsService() {
            return Mockito.mock(InvitationsService.class);
        }

        @Bean
        InvitationsController invitationsController(InvitationsService invitationsService) {
            return new InvitationsController(invitationsService);
        }
    }
}
