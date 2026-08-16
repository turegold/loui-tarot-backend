package com.louitarot.chemi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * POST /chemi-draws/{hostSlug}/guests 요청 바디. 비로그인 게스트가 자유 입력하는 이름/별명.
 *
 * 6자 제한: 케미 순위 리스트/별자리 뷰(ConstellationMap)에 이 이름이 좁은 자리에 그대로 표시되므로,
 * 프론트에서 입력 단계에 이미 6자로 막아뒀지만 서버도 동일하게 검증한다(경계 검증, 백엔드 설계 원칙 9번).
 */
public record ChemiGuestRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 6, message = "2~6자로 입력해주세요.")
        String nickname
) {
}
