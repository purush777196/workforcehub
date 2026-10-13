package com.workforcehub.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Custom CSRF Token Request Handler for SPA client compatibility.
 * Resolves token from X-XSRF-TOKEN header or standard request attribute.
 */
public final class SpaCsrfTokenRequestHandler extends CsrfTokenRequestAttributeHandler {
    private final CsrfTokenRequestHandler delegate = new CsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        this.delegate.handle(request, response, csrfToken);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader("X-XSRF-TOKEN");
        if (StringUtils.hasText(headerValue)) {
            return headerValue;
        }
        headerValue = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(headerValue)) ? headerValue : super.resolveCsrfTokenValue(request, csrfToken);
    }
}
