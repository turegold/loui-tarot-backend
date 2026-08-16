package com.louitarot.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** PATCH /users/me 요청 바디. */
public record UpdateNicknameRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 30, message = "2~30자로 입력해주세요.")
        String nickname
) {
}
