package com.louitarot.chemi.service;

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
import com.louitarot.chemi.dto.ChemiSummaryResponse;
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
 * 케미 뽑기(방장/게스트) 오케스트레이션 — 레이어드. 카드/유저 리포지토리를 직접 주입받는다.
 * 카드 해석·조합 해석 모두 AI를 호출하지 않고 CardInterpretationComposer /
 * ChemiCombinationInterpretationComposer가 즉시 조합한다([[AI 해석 캐싱 전략]] — AI는 나중에 다시 붙이기로 결정).
 */
@Service
public class ChemiService {

    private final ChemiDrawJpaRepository chemiDrawJpaRepository;
    private final ChemiJpaRepository chemiJpaRepository;
    private final ChemiCombinationJpaRepository chemiCombinationJpaRepository;
    private final CardJpaRepository cardJpaRepository;
    private final CardInterpretationJpaRepository cardInterpretationJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final String frontendUrl;

    public ChemiService(
            ChemiDrawJpaRepository chemiDrawJpaRepository,
            ChemiJpaRepository chemiJpaRepository,
            ChemiCombinationJpaRepository chemiCombinationJpaRepository,
            CardJpaRepository cardJpaRepository,
            CardInterpretationJpaRepository cardInterpretationJpaRepository,
            UserJpaRepository userJpaRepository,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.chemiDrawJpaRepository = chemiDrawJpaRepository;
        this.chemiJpaRepository = chemiJpaRepository;
        this.chemiCombinationJpaRepository = chemiCombinationJpaRepository;
        this.cardJpaRepository = cardJpaRepository;
        this.cardInterpretationJpaRepository = cardInterpretationJpaRepository;
        this.userJpaRepository = userJpaRepository;
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

    /** 방장의 공유 링크로 들어온 게스트의 뽑기. 뽑는 즉시 host와의 케미도 함께 계산한다. 전부 즉시 계산이라 병렬화가 필요 없다. */
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

        NormalizedPair normalized = normalizeOrder(
                hostCard.getId(), hostDraw.isReversed(), guestCard.getId(), guestReversed);
        short score = ChemiScoreCalculator.calculate(hostCard, hostDraw.isReversed(), guestCard, guestReversed);
        ChemiCombinationEntity combination = getOrGenerateCombination(
                hostCard, hostDraw.isReversed(), guestCard, guestReversed, score, normalized);

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

    /** 마이페이지 "내 기록"의 케미 뽑기 목록 — 로그인 방장으로 뽑은 것만(게스트 draw는 대상 아님). */
    @Transactional(readOnly = true)
    public Page<ChemiSummaryResponse> getMyChemiDraws(Long userId, Pageable pageable) {
        return chemiDrawJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(draw -> ChemiSummaryResponse.from(draw, findCard(draw.getCardId())));
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

    /** 캐시 조회, 미스면 조합+저장까지 한 번에. */
    private String getOrGenerateCardInterpretation(CardEntity card, boolean reversed) {
        String cachedText = findCachedCardInterpretation(card, reversed);
        if (cachedText != null) {
            return cachedText;
        }
        String text = CardInterpretationComposer.compose(card, reversed);
        cacheCardInterpretation(card, reversed, text);
        return text;
    }

    /** 캐시 조회만(DB 읽기). 미스면 null. */
    private String findCachedCardInterpretation(CardEntity card, boolean reversed) {
        return cardInterpretationJpaRepository.findByCardIdAndReversed(card.getId(), reversed)
                .map(CardInterpretationEntity::getInterpretationText)
                .orElse(null);
    }

    /** 새로 생성된 해석을 캐시에 저장. */
    private void cacheCardInterpretation(CardEntity card, boolean reversed, String text) {
        try {
            cardInterpretationJpaRepository.save(new CardInterpretationEntity(card.getId(), reversed, text));
        } catch (DataIntegrityViolationException e) {
            // 같은 (카드, 방향) 조합을 동시에 처음 요청한 다른 트랜잭션과 경합 — 먼저 저장된 캐시가 있으면 무시하고 넘어간다.
            if (findCachedCardInterpretation(card, reversed) == null) {
                throw e;
            }
        }
    }

    /** 캐시 조회, 미스면 조합+저장까지 한 번에. */
    private ChemiCombinationEntity getOrGenerateCombination(
            CardEntity cardA, boolean reversedA, CardEntity cardB, boolean reversedB, short score, NormalizedPair normalized) {
        ChemiCombinationEntity cached = findCombination(normalized).orElse(null);
        if (cached != null) {
            return cached;
        }
        String text = ChemiCombinationInterpretationComposer.compose(cardA, reversedA, cardB, reversedB, score);
        return cacheCombination(cardA.getId(), reversedA, cardB.getId(), reversedB, score, text, normalized);
    }

    /** 새로 생성된 조합 해석을 캐시에 저장. */
    private ChemiCombinationEntity cacheCombination(
            Short cardAId, boolean reversedA, Short cardBId, boolean reversedB,
            short score, String text, NormalizedPair normalized) {
        ChemiCombinationEntity combination = ChemiCombinationEntity.normalize(cardAId, reversedA, cardBId, reversedB, score, text);
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
}
