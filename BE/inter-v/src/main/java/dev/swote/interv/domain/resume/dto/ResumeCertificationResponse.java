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
@Schema(description = "자격증 정보 응답 DTO")
public class ResumeCertificationResponse {

    @Schema(description = "자격증 고유 ID", example = "1")
    private Integer id;

    @Schema(description = "자격증명", example = "정보처리기사")
    private String certificationName;

    @Schema(description = "발급 기관명", example = "한국산업인력공단")
    private String issuingOrganization;

    @Schema(
            description = "취득일",
            example = "2022-05-15",
            type = "string",
            format = "date"
    )
    private LocalDate acquiredDate;

    @Schema(
            description = "만료일 (무기한인 경우 null)",
            example = "2027-05-15",
            type = "string",
            format = "date"
    )
    private LocalDate expiryDate;

    @Schema(description = "무기한 여부", example = "true")
    private Boolean noExpiry;
}