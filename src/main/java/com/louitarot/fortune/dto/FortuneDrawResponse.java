package com.louitarot.fortune.dto;

import com.louitarot.common.domain.Topic;
import com.louitarot.fortune.entity.FortuneDrawEntity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * [[API 명세]]의 {@code POST /fortunes} / {@code GET /fortunes/{slug}} 공통 응답.
 * nickname은 GET에서만 채워진다(작성자 표시용) — POST는 방금 뽑은 본인이 보는 응답이라 불필요.
 */
public record FortuneDrawResponse(
        String slug,
        Topic topic,
        String spreadThemeKey,
        List<FortuneCardResponse> cards,
        String overallInterpretation,
        LocalDateTime createdAt,
        String nickname
) {

    public static FortuneDrawResponse withoutNickname(
            FortuneDrawEntity draw, List<FortuneCardResponse> cards, String overallInterpretation) {
        return new FortuneDrawResponse(
                draw.getSlug(), draw.getTopic(), draw.getSpreadThemeKey(), cards, overallInterpretation,
                draw.getCreatedAt(), null);
    }

    public static FortuneDrawResponse of(
            FortuneDrawEntity draw, List<FortuneCardResponse> cards, String overallInterpretation, String nickname) {
        return new FortuneDrawResponse(
                draw.getSlug(), draw.getTopic(), draw.getSpreadThemeKey(), cards, overallInterpretation,
                draw.getCreatedAt(), nickname);
    }
}
