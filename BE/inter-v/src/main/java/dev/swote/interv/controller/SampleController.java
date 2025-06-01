package dev.swote.interv.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.global.ResponseCode;
import dev.swote.interv.interceptor.CurrentUser;
import dev.swote.interv.service.user.UserService;
import dev.swote.interv.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Sample", description = "Sample API")
public class SampleController {

    private final UserService userService;

    @GetMapping("/sample")
    @Operation(summary = "샘플", description = "기본 요청입니다.")
    public ResponseEntity<CommonResponse<String>> sample(
            @RequestParam(name = "id", defaultValue = "1")
            @Parameter(name = "id", description = "테이블의 고유 키(id)", example = "1")
            Integer id
    ) {
        return ResponseEntity.ok(CommonResponse.ok("Sample response for ID: " + id));
    }

    @GetMapping("/error-sample")
    @Operation(summary = "에러", description = "에러 발생 샘플입니다.")
    public ResponseEntity<CommonResponse<String>> errorSample() throws Exception {
        throw new RuntimeException("Sample error");
    }

    @GetMapping("/error-handling-sample")
    @Operation(summary = "에러 핸들링", description = "에러 핸들링 샘플입니다.")
    public ResponseEntity<CommonResponse<String>> errorHandlingSample() {
        try {
            throw new RuntimeException("Sample error");
        } catch(Exception e) {
            log.error("error", e);
            return ResponseEntity.status(500)
                    .body(CommonResponse.ok("Error handled: " + e.getMessage()));
        }
    }

    @GetMapping("/login-required-sample")
    @Operation(summary = "로그인 필요한 요청", description = "로그인 정보가 필요한 요청 샘플입니다.")
    public ResponseEntity<CommonResponse<User>> loginRequiredSample(
            @Parameter(hidden = true) CurrentUser currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getUserById(currentUser.id());
        return ResponseEntity.ok(CommonResponse.ok(user));
    }
}