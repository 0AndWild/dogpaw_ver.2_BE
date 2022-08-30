package com.project.dogfaw.sse.security.jwt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "JWT 토큰 Dto")
@Getter
@NoArgsConstructor
public class TokenRequestDto {
    @Schema(description = "엑세스토큰")
    private String accessToken;
    @Schema(description = "리프레시토큰(재발급용)")
    private String refreshToken;
    private Long userId;
}