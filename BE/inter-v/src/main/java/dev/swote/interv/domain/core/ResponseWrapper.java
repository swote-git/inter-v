package dev.swote.interv.domain.core;
import lombok.Builder;

@Builder
public record ResponseWrapper(String message) {
}