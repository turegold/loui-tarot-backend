package com.louitarot.chemi.service;

import com.louitarot.ai.application.port.out.AiInterpretationPort;
import com.louitarot.auth.entity.UserEntity;
import com.louitarot.auth.repository.UserJpaRepository;
import com.louitarot.card.dto.CardBriefResponse;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.card.entity.CardInterpretationEntity;
import com.louitarot.card.repository.CardInterpretationJpaRepository;
import com.louitarot.card.repository.CardJpaRepository;
import com.louitarot.chemi.dto.ChemiDrawDetailResponse;
import com.louitarot.chemi.dto.ChemiDrawSummaryResponse;
import com.louitarot.chemi.dto.ChemiGuestDrawResponse;
import com.louitarot.chemi.dto.ChemiRankingItemResponse;
import com.louitarot.chemi.dto.ChemiResultResponse;
import com.louitarot.chemi.entity.ChemiCombinationEntity;
import com.louitarot.chemi.entity.ChemiDrawEntity;
import com.louitarot.chemi.entity.ChemiEntity;
import com.louitarot.chemi.repository.ChemiCombinationJpaRepository;
import com.louitarot.chemi.repository.ChemiDrawJpaRepository;
import com.louitarot.chemi.repository.ChemiJpaRepository;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import com.louitarot.common.util.SlugGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 케미 뽑기(방장/게스트) 오케스트레이션 — 레이어드. 카드/유저 리포지토리를 직접 주입받고,
 * AI 호출은 AiInterpretationPort(헥사고날)로 위임한다 ([[백엔드 설계 원칙]] 1번).
 */
@Service
public class ChemiService {

    private final ChemiDrawJpaRepository chemiDrawJpaRepository;
    private final ChemiJpaRepository chemiJpaRepository;
    private final ChemiCombinationJpaRepository chemiCombinationJpaRepository;
    private final CardJpaRepository cardJpaRepository;
    private final CardInterpretationJpaRepository cardInterpretationJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final AiInterpretationPort aiInterpretationPort;
    private final String frontendUrl;

    public ChemiService(
            ChemiDrawJpaRepository chemiDrawJpaRepository,
            ChemiJpaRepository chemiJpaRepository,
            ChemiCombinationJpaRepository chemiCombinationJpaRepository,
            CardJpaRepository cardJpaRepository,
            CardInterpretationJpaRepository cardInterpretationJpaRepository,
            UserJpaRepository userJpaRepository,
            AiInterpretationPort aiInterpretationPort,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.chemiDrawJpaRepository = chemiDrawJpaRepository;
        this.chemiJpaRepository = chemiJpaRepository;
        this.chemiCombinationJpaRepository = chemiCombinationJpaRepository;
        this.cardJpaRepository = cardJpaRepository;
        this.cardInterpretationJpaRepository = cardInterpretationJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.aiInterpretationPort = aiInterpretationPort;
        this.frontendUrl = frontendUrl;
    }

    /** 방장(로그인 계정)이 홈 화면에서 시작하는 최초 뽑기. */
    @Transactional
    public ChemiDrawDetailResponse createHostDraw(Long userId) {
        UserEntity user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));

        CardEntity card = pickRandomCard();
        boolean reversed = ThreadLocalRandom.current().nextBoolean();
        ChemiDrawEntity draw = new ChemiDrawEntity(
                generateUniqueSlug(), userId, user.getNickname(), card.getId(), reversed, null);
        chemiDrawJpaRepository.save(draw);

        String interpretation = getOrGenerateCardInterpretation(card, reversed);
        return ChemiDrawDetailResponse.withoutChemi(draw, card, interpretation, shareUrl(draw.getSlug()));
    }

    /** 방장의 공유 링크로 들어온 게스트의 뽑기. 뽑는 즉시 host와의 케미도 함께 계산한다. */
    @Transactional
    public ChemiGuestDrawResponse createGuestDraw(String hostSlug, String nickname, String ipHash) {
        ChemiDrawEntity hostDraw = chemiDrawJpaRepository.findBySlug(hostSlug)
                .orElseThrow(() -> new CustomException(ErrorCode.CHEMI_HOST_NOT_FOUND));
        CardEntity hostCard = findCard(hostDraw.getCardId());

        CardEntity guestCard = pickRandomCard();
        boolean guestReversed = ThreadLocalRandom.current().nextBoolean();
        ChemiDrawEntity guestDraw = new ChemiDrawEntity(
                generateUniqueSlug(), null, nickname, guestCard.getId(), guestReversed, ipHash);
        chemiDrawJpaRepository.save(guestDraw);

        String guestInterpretation = getOrGenerateCardInterpretation(guestCard, guestReversed);
        ChemiCombinationEntity combination = getOrGenerateCombination(
                hostCard, hostDraw.isReversed(), guestCard, guestReversed);
        chemiJpaRepository.save(new ChemiEntity(hostDraw, guestDraw, combination, combination.getScore()));

        return new ChemiGuestDrawResponse(
                ChemiDrawSummaryResponse.of(guestDraw, guestCard, guestInterpretation),
                ChemiDrawSummaryResponse.withoutInterpretation(hostDraw, hostCard),
                new ChemiResultResponse(combination.getScore(), combination.getInterpretationText()));
    }

    /** 뽑기 결과 단건 조회. 이 draw가 게스트로 참여한 적이 있다면 host와의 케미 결과도 함께 반환. */
    @Transactional
    public ChemiDrawDetailResponse getDraw(String slug) {
        ChemiDrawEntity draw = chemiDrawJpaRepository.findBySlug(slug)
                .orElseThrow(() -> new CustomException(ErrorCode.CHEMI_DRAW_NOT_FOUND));
        CardEntity card = findCard(draw.getCardId());
        String interpretation = getOrGenerateCardInterpretation(card, draw.isReversed());
        String shareUrl = shareUrl(draw.getSlug());

        Optional<ChemiEntity> chemiAsGuest = chemiJpaRepository.findByGuestDrawId(draw.getId());
        if (chemiAsGuest.isEmpty()) {
            return ChemiDrawDetailResponse.withoutChemi(draw, card, interpretation, shareUrl);
        }

        ChemiEntity chemi = chemiAsGuest.get();
        ChemiDrawEntity hostDraw = chemi.getHostDraw();
        CardEntity hostCard = findCard(hostDraw.getCardId());
        return ChemiDrawDetailResponse.of(
                draw, card, interpretation, shareUrl,
                ChemiDrawSummaryResponse.withoutInterpretation(hostDraw, hostCard),
                new ChemiResultResponse(chemi.getScore(), chemi.getCombination().getInterpretationText()));
    }

    /** 방장 기준 케미 순위 리스트(2차 기능). 점수 내림차순 — idx_host_score 인덱스를 그대로 탄다. */
    @Transactional(readOnly = true)
    public Page<ChemiRankingItemResponse> getRanking(String hostSlug, Pageable pageable) {
        ChemiDrawEntity hostDraw = chemiDrawJpaRepository.findBySlug(hostSlug)
                .orElseThrow(() -> new CustomException(ErrorCode.CHEMI_DRAW_NOT_FOUND));
        return chemiJpaRepository.findByHostDrawIdOrderByScoreDesc(hostDraw.getId(), pageable)
                .map(chemi -> new ChemiRankingItemResponse(
                        chemi.getGuestDraw().getNickname(),
                        CardBriefResponse.from(findCard(chemi.getGuestDraw().getCardId())),
                        chemi.getScore()));
    }

    private CardEntity pickRandomCard() {
        List<CardEntity> cards = cardJpaRepository.findAll();
        return cards.get(ThreadLocalRandom.current().nextInt(cards.size()));
    }

    private CardEntity findCard(Short cardId) {
        return cardJpaRepository.findById(cardId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_NOT_FOUND));
    }

    private String generateUniqueSlug() {
        String slug;
        do {
            slug = SlugGenerator.generate();
        } while (chemiDrawJpaRepository.existsBySlug(slug));
        return slug;
    }

    private String shareUrl(String slug) {
        return frontendUrl + "/chemi/" + slug;
    }

    private String getOrGenerateCardInterpretation(CardEntity card, boolean reversed) {
        return cardInterpretationJpaRepository.findByCardIdAndReversed(card.getId(), reversed)
                .map(CardInterpretationEntity::getInterpretationText)
                .orElseGet(() -> generateAndCacheCardInterpretation(card, reversed));
    }

    private String generateAndCacheCardInterpretation(CardEntity card, boolean reversed) {
        String prompt = "당신은 타로 카드 해석가입니다. '%s' 카드가 %s으로 나왔을 때의 일반적인 의미를 2~3문장으로 해석해주세요."
                .formatted(card.getNameKr(), reversed ? "역방향" : "정방향");
        String text = aiInterpretationPort.generate(prompt);
        try {
            cardInterpretationJpaRepository.save(new CardInterpretationEntity(card.getId(), reversed, text));
            return text;
        } catch (DataIntegrityViolationException e) {
            // 같은 (카드, 방향) 조합을 동시에 처음 요청한 다른 트랜잭션과 경합 — 먼저 저장된 캐시를 그대로 쓴다.
            return cardInterpretationJpaRepository.findByCardIdAndReversed(card.getId(), reversed)
                    .map(CardInterpretationEntity::getInterpretationText)
                    .orElseThrow(() -> e);
        }
    }

    private ChemiCombinationEntity getOrGenerateCombination(
            CardEntity cardA, boolean reversedA, CardEntity cardB, boolean reversedB) {
        NormalizedPair normalized = normalizeOrder(cardA.getId(), reversedA, cardB.getId(), reversedB);
        return findCombination(normalized)
                .orElseGet(() -> generateAndCacheCombination(cardA, reversedA, cardB, reversedB, normalized));
    }

    private ChemiCombinationEntity generateAndCacheCombination(
            CardEntity cardA, boolean reversedA, CardEntity cardB, boolean reversedB, NormalizedPair normalized) {
        short score = ChemiScoreCalculator.calculate(cardA, reversedA, cardB, reversedB);
        String prompt = """
                당신은 타로 궁합 해석가입니다. 두 사람이 각각 뽑은 카드는 '%s'(%s)와 '%s'(%s)이고, \
                궁합 점수는 %d점으로 '%s' 등급입니다. 두 사람의 케미를 2~3문장으로 따뜻하고 재미있게 해석해주세요.\
                """.formatted(
                        cardA.getNameKr(), reversedA ? "역방향" : "정방향",
                        cardB.getNameKr(), reversedB ? "역방향" : "정방향",
                        score, toneLabel(score));
        String text = aiInterpretationPort.generate(prompt);
        ChemiCombinationEntity combination = ChemiCombinationEntity.normalize(
                cardA.getId(), reversedA, cardB.getId(), reversedB, score, text);
        try {
            return chemiCombinationJpaRepository.save(combination);
        } catch (DataIntegrityViolationException e) {
            // 같은 카드 조합을 동시에 처음 요청한 다른 트랜잭션과 경합 — 먼저 저장된 캐시를 그대로 쓴다.
            return findCombination(normalized).orElseThrow(() -> e);
        }
    }

    private Optional<ChemiCombinationEntity> findCombination(NormalizedPair pair) {
        return chemiCombinationJpaRepository.findByCardAIdAndReversedAAndCardBIdAndReversedB(
                pair.cardAId(), pair.reversedA(), pair.cardBId(), pair.reversedB());
    }

    /** ChemiCombinationEntity.normalize()와 동일한 (a ≤ b) 정렬 규칙을 조회에도 그대로 적용하기 위한 값. */
    private record NormalizedPair(Short cardAId, boolean reversedA, Short cardBId, boolean reversedB) {
    }

    private NormalizedPair normalizeOrder(Short cardAId, boolean reversedA, Short cardBId, boolean reversedB) {
        boolean inOrder = cardAId < cardBId || (cardAId.equals(cardBId) && !reversedA);
        return inOrder
                ? new NormalizedPair(cardAId, reversedA, cardBId, reversedB)
                : new NormalizedPair(cardBId, reversedB, cardAId, reversedA);
    }

    private String toneLabel(short score) {
        if (score >= 90) {
            return "천생연분";
        }
        if (score >= 70) {
            return "잘 맞음";
        }
        if (score >= 50) {
            return "무난";
        }
        if (score >= 30) {
            return "노력 필요";
        }
        return "상극";
    }
}
