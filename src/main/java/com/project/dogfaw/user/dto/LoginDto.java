package com.project.dogfaw.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
@Schema(description = "로그인 Dto")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginDto {

    @Schema(description = "유저 이메일")
    private String username;
    @Schema(description = "유저 비밀번호")
    private String password;

    public UsernamePasswordAuthenticationToken toAuthentication() {
        return new UsernamePasswordAuthenticationToken(username, password);
    }
}
