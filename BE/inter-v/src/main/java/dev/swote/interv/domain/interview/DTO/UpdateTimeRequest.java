package dev.swote.interv.domain.interview.DTO;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UpdateTimeRequest {
    private Integer timeInSeconds;
}