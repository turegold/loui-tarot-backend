package com.louitarot.chemi.dto;

import com.louitarot.card.dto.CardBriefResponse;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.chemi.entity.ChemiDrawEntity;

import java.time.LocalDateTime;

/**
 * [[API 명세]]의 {@code POST /chemi-draws}(방장 뽑기) 및 {@code GET /chemi-draws/{slug}}
 * 응답에 공통으로 쓰는 형태. 방장이 막 뽑은 시점엔 아직 아무와도 짝지어지지 않았으니
 * {@code hostDraw}/{@code chemi}가 null이고, 이 draw가 게스트로 참여한 적이 있다면
 * (GET 조회 시) 그 상대 host와 케미 결과가 채워진다.
 */
public record ChemiDrawDetailResponse(
        String slug,
        String nickname,
        CardBriefResponse card,
        boolean isReversed,
        String interpretation,
        String shareUrl,
        LocalDateTime createdAt,
        ChemiDrawSummaryResponse hostDraw,
        ChemiResultResponse chemi
) {

    /** 방장이 막 뽑은 직후 — 아직 케미 상대가 없다. */
    public static ChemiDrawDetailResponse hostCreated(
            ChemiDrawEntity draw, CardEntity card, String interpretation, String shareUrl) {
        return new ChemiDrawDetailResponse(
                draw.getSlug(), draw.getNickname(), CardBriefResponse.from(card), draw.isReversed(),
                interpretation, shareUrl, draw.getCreatedAt(), null, null);
    }

    /** 단건 조회(GET) — 이 draw가 게스트로 참여했다면 host/chemi가 채워진 상태로 넘어온다. */
    public static ChemiDrawDetailResponse of(
            ChemiDrawEntity draw, CardEntity card, String interpretation, String shareUrl,
            ChemiDrawSummaryResponse hostDraw, ChemiResultResponse chemi) {
        return new ChemiDrawDetailResponse(
                draw.getSlug(), draw.getNickname(), CardBriefResponse.from(card), draw.isReversed(),
                interpretation, shareUrl, draw.getCreatedAt(), hostDraw, chemi);
    }
}
