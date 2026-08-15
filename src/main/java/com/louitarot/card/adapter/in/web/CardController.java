package com.louitarot.card.adapter.in.web;

import com.louitarot.card.application.port.in.GetCardUseCase;
import com.louitarot.card.application.port.in.GetCardsUseCase;
import com.louitarot.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [[API 명세]]의 카드(Cards) 엔드포인트. 인증 불필요 — 78장 전부와 SEO 상세 페이지에 쓰인다.
 * 컨트롤러는 유스케이스 인터페이스(GetCardsUseCase/GetCardUseCase)에만 의존한다 —
 * CardService라는 구체 클래스를 직접 알지 못한다.
 */
@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final GetCardsUseCase getCardsUseCase;
    private final GetCardUseCase getCardUseCase;

    public CardController(GetCardsUseCase getCardsUseCase, GetCardUseCase getCardUseCase) {
        this.getCardsUseCase = getCardsUseCase;
        this.getCardUseCase = getCardUseCase;
    }

    @GetMapping
    public ApiResponse<List<CardSummaryResponse>> getCards() {
        List<CardSummaryResponse> cards = getCardsUseCase.getCards().stream()
                .map(CardSummaryResponse::from)
                .toList();
        return ApiResponse.success(cards);
    }

    @GetMapping("/{cardId}")
    public ApiResponse<CardDetailResponse> getCard(@PathVariable Short cardId) {
        return ApiResponse.success(CardDetailResponse.from(getCardUseCase.getCard(cardId)));
    }
}
