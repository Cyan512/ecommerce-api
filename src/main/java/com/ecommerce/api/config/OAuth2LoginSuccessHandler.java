package com.ecommerce.api.config;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.ecommerce.api.dto.enums.UserRole;
import com.ecommerce.api.model.User;
import com.ecommerce.api.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();

        OAuth2User oauthUser = token.getPrincipal();
        String providerId = oauthUser.getName();
        String name = oauthUser.getAttribute("name");
        String email = oauthUser.getAttribute("email");
        String imageUrl = oauthUser.getAttribute("picture");

        if ("github".equals(provider)) {
            imageUrl = oauthUser.getAttribute("avatar_url");
        }

        User user = userRepository.findByEmail(email)
                .orElse(User.builder()
                        .provider(provider)
                        .providerId(providerId)
                        .email(email)
                        .role(UserRole.USER)
                        .build());

        user.setName(name);
        user.setImageUrl(imageUrl);
        user.setProvider(provider);
        user.setProviderId(providerId);

        userRepository.save(user);

        OAuth2AuthenticationToken updatedToken = new OAuth2AuthenticationToken(
                oauthUser,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                provider);

        SecurityContextHolder.getContext().setAuthentication(updatedToken);

        response.sendRedirect("http://localhost:8080");
    }

}
