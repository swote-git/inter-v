package dev.swote.interv.domain.resume.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "자격증 정보 요청 DTO")
public class ResumeCertificationRequest {

    @JsonProperty("certificationId")
    @Schema(
            description = "자격증 ID (수정시에만 사용, 신규 생성시에는 null)",
            example = "1"
    )
    private Integer id;

    @NotBlank(message = "자격증명은 필수입니다")
    @Size(max = 100, message = "자격증명은 100자 이하로 입력해주세요")
    @Schema(description = "자격증명", example = "정보처리기사", required = true)
    private String certificationName;

    @NotBlank(message = "발급기관은 필수입니다")
    @Size(max = 100, message = "발급기관은 100자 이하로 입력해주세요")
    @Schema(description = "자격증 발급 기관명", example = "한국산업인력공단", required = true)
    private String issuingOrganization;

    @Schema(description = "자격증 취득일", example = "2022-05-15")
    private LocalDate acquiredDate;

    @Schema(description = "자격증 만료일", example = "2027-05-15")
    private LocalDate expiryDate;

    @Schema(description = "무기한 여부", example = "true")
    private Boolean noExpiry;
}
