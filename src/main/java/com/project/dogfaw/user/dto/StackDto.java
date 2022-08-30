package com.project.dogfaw.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@Schema(description = "기술 스택 Dto")
@AllArgsConstructor
public class StackDto {
    private String stack;

}
