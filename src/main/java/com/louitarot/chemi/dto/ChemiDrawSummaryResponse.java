package com.louitarot.chemi.dto;

import com.louitarot.card.dto.CardBriefResponse;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.chemi.entity.ChemiDrawEntity;

/**
 * 케미 뽑기 한 건을 요약해서 실을 때 쓰는 공통 형태. 다른 draw에 embed될 때(게스트 생성
 * 응답의 hostDraw처럼) interpretation/shareUrl을 굳이 안 채워도 되는 자리에서는 null로
 * 둔다 — "값이 없다"가 아니라 "이 응답에서는 필요 없어서 안 채웠다"는 의도적인 null.
 * 게스트 뽑기 응답의 guestDraw처럼 "이 링크로 내 케미도 공유하기"가 필요한 자리에서는
 * shareUrl을 채워서 내려준다.
 */
public record ChemiDrawSummaryResponse(
        String slug,
        String nickname,
        CardBriefResponse card,
        boolean isReversed,
        String interpretation,
        String shareUrl
) {

    public static ChemiDrawSummaryResponse of(ChemiDrawEntity draw, CardEntity card, String interpretation, String shareUrl) {
        return new ChemiDrawSummaryResponse(
                draw.getSlug(), draw.getNickname(), CardBriefResponse.from(card), draw.isReversed(), interpretation, shareUrl);
    }

    public static ChemiDrawSummaryResponse withoutInterpretation(ChemiDrawEntity draw, CardEntity card) {
        return of(draw, card, null, null);
    }
}
