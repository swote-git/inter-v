package dev.swote.interv.domain.interview.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "면접 시간 업데이트 요청 DTO")
public class UpdateTimeRequest {

    @Schema(
            description = "면접에 소요된 총 시간 (초 단위)",
            example = "1800",
            minimum = "0",
            maximum = "10800"
    )
    private Integer timeInSeconds;
}