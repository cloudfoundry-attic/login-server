package org.cloudfoundry.identity.uaa.login;

import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class XFrameOptionsFilter extends OncePerRequestFilter {

    private final XFrameOptionsHeaderWriter xFrameOptionsHeaderWriter;

    public XFrameOptionsFilter(XFrameOptionsHeaderWriter xFrameOptionsHeaderWriter) {
        super();
        this.xFrameOptionsHeaderWriter = xFrameOptionsHeaderWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        xFrameOptionsHeaderWriter.writeHeaders(request, response);
        filterChain.doFilter(request, response);
    }
}
