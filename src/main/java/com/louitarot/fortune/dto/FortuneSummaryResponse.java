package com.louitarot.fortune.dto;

import com.louitarot.common.domain.Topic;
import com.louitarot.fortune.entity.FortuneDrawEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [[API 명세]]의 {@code GET /users/me/fortunes} 목록 항목 하나. {@link FortuneDrawResponse}와 달리
 * 해석 텍스트는 담지 않는다(목록에서는 불필요, 상세는 GET /fortunes/{slug}) — 그래서 안의
 * {@link FortuneCardResponse}들도 interpretation이 항상 null이다.
 */
public record FortuneSummaryResponse(
        String slug,
        Topic topic,
        List<FortuneCardResponse> cards,
        LocalDateTime createdAt
) {

    public static FortuneSummaryResponse from(FortuneDrawEntity draw, List<FortuneCardResponse> cards) {
        return new FortuneSummaryResponse(draw.getSlug(), draw.getTopic(), cards, draw.getCreatedAt());
    }
}
