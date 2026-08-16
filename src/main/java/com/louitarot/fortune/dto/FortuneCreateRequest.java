package com.louitarot.fortune.dto;

import com.louitarot.common.domain.Topic;
import jakarta.validation.constraints.NotNull;

/** POST /fortunes 요청 바디. */
public record FortuneCreateRequest(
        @NotNull(message = "주제는 필수입니다.") Topic topic
) {
}
