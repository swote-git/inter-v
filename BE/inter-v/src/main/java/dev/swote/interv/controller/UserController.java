package dev.swote.interv.controller;

import dev.swote.interv.config.CommonResponse;
import dev.swote.interv.domain.user.VO.RegisterVO;
import dev.swote.interv.domain.user.entity.User;
import dev.swote.interv.interceptor.CurrentUser;
import dev.swote.interv.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<CommonResponse<User>> register(@Valid @RequestBody RegisterVO registerVO) {
        User user = userService.register(registerVO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonResponse.ok(user));
    }

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<User>> getMyProfile(CurrentUser currentUser) {
        User user = userService.getUserById(currentUser.id());
        return ResponseEntity.ok(CommonResponse.ok(user));
    }

    @PutMapping("/me")
    public ResponseEntity<CommonResponse<User>> updateMyProfile(
            CurrentUser currentUser,
            @RequestBody User updatedUser
    ) {
        User user = userService.updateUser(currentUser.id(), updatedUser);
        return ResponseEntity.ok(CommonResponse.ok(user));
    }
}