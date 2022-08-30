package com.project.dogfaw.user.dto;

import com.project.dogfaw.user.model.UserRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "회원가입 요청Dto")
@Getter
public class SignupRequestDto {
    private Long userId;
    @Schema(description = "이메일")
    private String username;
    @Schema(description = "비밀번호")
    private String password;
    @Schema(description = "비밀번호 확인")
    private String passwordConfirm;
    @Schema(description = "사용할 닉네임")
    private String nickname;
    @Schema(description = "기술스택 리스트(선택)")
    private List<StackDto> stacks = new ArrayList<>();
}
