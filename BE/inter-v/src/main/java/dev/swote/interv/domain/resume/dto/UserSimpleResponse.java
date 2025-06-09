package dev.swote.interv.domain.resume.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 기본 정보 DTO (민감한 정보 제외)")
public class UserSimpleResponse {

    @Schema(description = "사용자 고유 ID", example = "1")
    private Integer id;

    @Schema(
            description = "사용자 실명",
            example = "홍길동",
            maxLength = 50
    )
    private String userName;

    @Schema(
            description = "사용자 닉네임",
            example = "개발자홍길동",
            maxLength = 30
    )
    private String nickname;

    @Schema(
            description = "생년월일",
            example = "1990-01-01",
            type = "string",
            format = "date"
    )
    private LocalDate birthDate;
}