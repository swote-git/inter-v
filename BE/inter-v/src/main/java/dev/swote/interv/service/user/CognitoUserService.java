package dev.swote.interv.service.user;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;

import dev.swote.interv.domain.user.entity.CognitoUserDetails;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CognitoUserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AWSCognitoIdentityProvider cognitoClient;
    private final String userPoolId;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));

        return new CognitoUserDetails(user);
    }

    @Transactional
    public UserDetails loadOrCreateUser(String cognitoUsername, Jwt jwt) {
        // Cognito 사용자 정보에서 이메일 가져오기
        String email = jwt.getClaimAsString("email");

        // 이메일로 기존 사용자 찾기
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // 기존 사용자가 있으면 업데이트 (필요시)
            User user = existingUser.get();
            // 필요한 사용자 정보 업데이트
            return new CognitoUserDetails(user);
        } else {
            // 새 사용자 생성
            return createNewUser(cognitoUsername, email, jwt);
        }
    }

    @Transactional
    public UserDetails createNewUser(String cognitoUsername, String email, Jwt jwt) {
        try {
            // Cognito에서 사용자 정보 가져오기
            AdminGetUserRequest userRequest = new AdminGetUserRequest()
                    .withUsername(cognitoUsername)
                    .withUserPoolId(userPoolId);

            AdminGetUserResult userResult = cognitoClient.adminGetUser(userRequest);

            // 사용자 정보 추출
            String nickname = getAttributeValue(userResult, "nickname", cognitoUsername);
            String phoneNumber = getAttributeValue(userResult, "phone_number", "");
            String birthDateStr = getAttributeValue(userResult, "birthdate", "2000-01-01");
            LocalDate birthDate = LocalDate.parse(birthDateStr);

            // 사용자 생성
            User newUser = User.builder()
                    .email(email)
                    .userName(getAttributeValue(userResult, "name", ""))
                    .nickname(nickname)
                    .phoneNumber(phoneNumber)
                    .birthDate(birthDate)
                    .build();

            userRepository.save(newUser);

            return new CognitoUserDetails(newUser);
        } catch (Exception e) {
            log.error("Error creating user from Cognito: {}", e.getMessage());

            // 최소한의 정보로 사용자 생성
            User newUser = User.builder()
                    .email(email)
                    .userName(jwt.getClaimAsString("name"))
                    .nickname(email.split("@")[0])
                    .phoneNumber("")
                    .birthDate(LocalDate.now())
                    .build();

            userRepository.save(newUser);

            return new CognitoUserDetails(newUser);
        }
    }

    private String getAttributeValue(AdminGetUserResult userResult, String attributeName, String defaultValue) {
        return userResult.getUserAttributes().stream()
                .filter(attr -> attr.getName().equals(attributeName))
                .map(AttributeType::getValue)
                .findFirst()
                .orElse(defaultValue);
    }
}