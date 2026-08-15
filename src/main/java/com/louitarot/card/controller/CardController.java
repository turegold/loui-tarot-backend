package com.louitarot.card.controller;

import com.louitarot.card.dto.CardDetailResponse;
import com.louitarot.card.dto.CardSummaryResponse;
import com.louitarot.card.service.CardService;
import com.louitarot.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** [[API 명세]]의 카드(Cards) 엔드포인트. 인증 불필요 — 78장 전부와 SEO 상세 페이지에 쓰인다. */
@RestController
@RequestMapping("/api/v1/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @GetMapping
    public ApiResponse<List<CardSummaryResponse>> getCards() {
        List<CardSummaryResponse> cards = cardService.getCards().stream()
                .map(CardSummaryResponse::from)
                .toList();
        return ApiResponse.success(cards);
    }

    @GetMapping("/{cardId}")
    public ApiResponse<CardDetailResponse> getCard(@PathVariable Short cardId) {
        return ApiResponse.success(CardDetailResponse.from(cardService.getCard(cardId)));
    }
}
