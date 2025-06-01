package dev.swote.interv.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.user.entity.CognitoUserDetails;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Value("${aws.cognito.logout-redirect-uri}")
    private String logoutRedirectUri;

    @GetMapping("/login")
    public ResponseEntity<CommonResponse<String>> login() {
        String loginUrl = getLoginUrl();
        return ResponseEntity.ok(CommonResponse.ok(loginUrl));
    }

    @GetMapping("/login/success")
    public ResponseEntity<CommonResponse<Map<String, Object>>> loginSuccess(
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal CognitoUserDetails userDetails) {

        log.info("로그인 성공: {}", oidcUser.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("idToken", oidcUser.getIdToken().getTokenValue());

        if (userDetails != null) {
            User user = userDetails.getUser();
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            response.put("name", user.getUserName());
        } else {
            response.put("email", oidcUser.getEmail());
            response.put("name", oidcUser.getFullName());
        }

        return ResponseEntity.ok(CommonResponse.ok(response));
    }

    @GetMapping("/login/failure")
    public ResponseEntity<CommonResponse<String>> loginFailure() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(CommonResponse.ok("로그인에 실패했습니다."));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<String>> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        // SecurityContext에서 로그아웃
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        // Cognito 로그아웃 URL 생성
        String logoutUrl = getCognitoLogoutUrl();

        return ResponseEntity.ok(CommonResponse.ok(logoutUrl));
    }

    @GetMapping("/user")
    public ResponseEntity<CommonResponse<User>> getCurrentUser(@AuthenticationPrincipal CognitoUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userDetails.getUser();
        return ResponseEntity.ok(CommonResponse.ok(user));
    }

    private String getLoginUrl() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("cognito");

        return UriComponentsBuilder
                .fromUriString(clientRegistration.getProviderDetails().getAuthorizationUri())
                .queryParam("client_id", clientRegistration.getClientId())
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", clientRegistration.getScopes()))
                .queryParam("redirect_uri", clientRegistration.getRedirectUri())
                .build()
                .toUriString();
    }

    private String getCognitoLogoutUrl() {
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId("cognito");
        String domain = URI.create(clientRegistration.getProviderDetails().getIssuerUri()).getHost();

        return UriComponentsBuilder
                .fromHttpUrl("https://" + domain + "/logout")
                .queryParam("client_id", clientRegistration.getClientId())
                .queryParam("logout_uri", logoutRedirectUri)
                .build()
                .toUriString();
    }
}