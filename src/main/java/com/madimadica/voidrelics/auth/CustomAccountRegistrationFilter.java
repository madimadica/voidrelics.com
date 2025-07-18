package com.madimadica.voidrelics.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.madimadica.voidrelics.account.UserAccountService;
import com.madimadica.voidrelics.auth.dto.AuthenticatedUserDto;
import com.madimadica.voidrelics.auth.dto.UserAccountRegistrationDto;
import com.madimadica.voidrelics.exceptions.ApiError;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;

public class CustomAccountRegistrationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAccountRegistrationFilter.class);

    private final RequestMatcher registerMatcher = new AntPathRequestMatcher("/api/v1/account/register", "POST");
    private final ObjectMapper objectMapper;
    private final UserAccountService userAccountService;
    private final CustomLoginAuthenticationFilter loginAuthFilter;

    public CustomAccountRegistrationFilter(ObjectMapper objectMapper, UserAccountService userAccountService, CustomLoginAuthenticationFilter loginAuthFilter) {
        this.objectMapper = objectMapper;
        this.userAccountService = userAccountService;
        this.loginAuthFilter = loginAuthFilter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        this.doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (!registerMatcher.matches(request)) {
            chain.doFilter(request, response);
            return;
        }
        String requestBody = String.join("\n", request.getReader().lines().toList());
        UserAccountRegistrationDto dto = objectMapper.readValue(requestBody, UserAccountRegistrationDto.class);
        CustomUser userDetails;
        try {
            userDetails = userAccountService.register(dto).toCustomUser();
        } catch (ApiError e) {
            response.setStatus(e.status());
            response.setContentType("application/json");
            objectMapper.writeValue(response.getOutputStream(), e.toJson());
            return;
        }
        LOGGER.info("Registered new user {}", userDetails);
        // Automatically log in the newly registered user
        var auth = loginAuthFilter.loginHelper(request, response, dto.username(), dto.password());
        LOGGER.info("Logged in as new user {}", userDetails);
    }
}
