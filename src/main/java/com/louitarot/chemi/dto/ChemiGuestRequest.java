package com.louitarot.chemi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /chemi-draws/{hostSlug}/guests 요청 바디. 비로그인 게스트가 자유 입력하는 이름/별명. */
public record ChemiGuestRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 30, message = "2~30자로 입력해주세요.")
        String nickname
) {
}
