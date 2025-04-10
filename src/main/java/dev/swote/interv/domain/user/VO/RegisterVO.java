package dev.swote.interv.domain.user.VO;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegisterVO {

    @Email(message = "이메일 형식에 일치하지 않습니다. 입력하신 값을 다시 확인해주세요.")
    @NotEmpty(message = "이메일을 입력해주세요.")
    private String email;

    @Size(min = 8, message = "비밀번호 최소 길이는 8자리 입니다.")
    @NotEmpty(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotEmpty(message = "이름을 입력해주세요.")
    private String userName;

    @NotEmpty(message = "닉네임을 입력해주세요.")
    private String nickname;

    @Size(max = 16, message = "전화번호의 최대 길이는 16자 입니다.")
    @NotEmpty(message = "전화번호를 입력해주세요.")
    @Pattern(regexp = "^\\+[0-9]+$", message = "전화번호 형식을 지켜주세요.")
    private String phoneNumber;

    @NotNull(message = "생년월일을 입력해주세요.")
    private LocalDate birthDate;
}