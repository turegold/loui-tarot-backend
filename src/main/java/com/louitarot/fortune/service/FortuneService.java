package com.louitarot.fortune.service;

import com.louitarot.auth.entity.UserEntity;
import com.louitarot.auth.repository.UserJpaRepository;
import com.louitarot.card.dto.CardBriefResponse;
import com.louitarot.card.entity.CardEntity;
import com.louitarot.card.entity.CardTopicInterpretationEntity;
import com.louitarot.card.repository.CardJpaRepository;
import com.louitarot.card.repository.CardTopicInterpretationJpaRepository;
import com.louitarot.common.domain.Topic;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import com.louitarot.common.util.SlugGenerator;
import com.louitarot.fortune.domain.SpreadTheme;
import com.louitarot.fortune.dto.FortuneCardResponse;
import com.louitarot.fortune.dto.FortuneDrawResponse;
import com.louitarot.fortune.dto.FortuneSummaryResponse;
import com.louitarot.fortune.entity.FortuneDrawCardEntity;
import com.louitarot.fortune.entity.FortuneDrawEntity;
import com.louitarot.fortune.repository.FortuneDrawJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 개인 카드 뽑기(3장 스프레드) 오케스트레이션 — 레이어드. 케미 뽑기와 동일하게 카드/유저
 * 리포지토리를 직접 주입받는다. 카드별·종합 해석 모두 AI를 호출하지 않고 CardTopicInterpretationComposer /
 * OverallInterpretationComposer가 즉시 조합한다([[AI 해석 캐싱 전략]] — AI는 나중에 다시 붙이기로 결정).
 */
@Service
public class FortuneService {

    private static final int SPREAD_SIZE = 3;

    private final FortuneDrawJpaRepository fortuneDrawJpaRepository;
    private final CardJpaRepository cardJpaRepository;
    private final CardTopicInterpretationJpaRepository cardTopicInterpretationJpaRepository;
    private final UserJpaRepository userJpaRepository;

    public FortuneService(
            FortuneDrawJpaRepository fortuneDrawJpaRepository,
            CardJpaRepository cardJpaRepository,
            CardTopicInterpretationJpaRepository cardTopicInterpretationJpaRepository,
            UserJpaRepository userJpaRepository
    ) {
        this.fortuneDrawJpaRepository = fortuneDrawJpaRepository;
        this.cardJpaRepository = cardJpaRepository;
        this.cardTopicInterpretationJpaRepository = cardTopicInterpretationJpaRepository;
        this.userJpaRepository = userJpaRepository;
    }

    /** 3장을 한 번에 뽑아 카드별 해석 + 종합 해석까지 함께 생성한다. 전부 즉시 계산이라 병렬화가 필요 없다. */
    @Transactional
    public FortuneDrawResponse createDraw(Long userId, Topic topic) {
        if (!userJpaRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        SpreadTheme theme = SpreadTheme.resolveForToday();
        List<CardEntity> pickedCards = pickThreeDistinctCards();
        List<Boolean> reversedFlags = pickedCards.stream()
                .map(card -> ThreadLocalRandom.current().nextBoolean())
                .toList();

        List<String> cardTexts = new ArrayList<>(SPREAD_SIZE);
        for (int i = 0; i < SPREAD_SIZE; i++) {
            cardTexts.add(getOrGenerateCardTopicInterpretation(pickedCards.get(i), reversedFlags.get(i), topic));
        }
        String overallInterpretation = OverallInterpretationComposer.compose(topic, theme, pickedCards, reversedFlags);

        List<FortuneCardResponse> cardResponses = new ArrayList<>(SPREAD_SIZE);
        for (int i = 0; i < SPREAD_SIZE; i++) {
            cardResponses.add(new FortuneCardResponse(
                    theme.labelAt(i), CardBriefResponse.from(pickedCards.get(i)), reversedFlags.get(i), cardTexts.get(i)));
        }

        FortuneDrawEntity draw = new FortuneDrawEntity(
                generateUniqueSlug(), userId, topic, theme.key(), overallInterpretation);
        for (int i = 0; i < SPREAD_SIZE; i++) {
            draw.addCard(new FortuneDrawCardEntity((short) i, pickedCards.get(i).getId(), reversedFlags.get(i)));
        }
        fortuneDrawJpaRepository.save(draw);

        return FortuneDrawResponse.withoutNickname(draw, cardResponses, overallInterpretation);
    }

    /** 결과 공유 링크 조회. 로그인 없이도 볼 수 있다. */
    @Transactional(readOnly = true)
    public FortuneDrawResponse getDraw(String slug) {
        FortuneDrawEntity draw = fortuneDrawJpaRepository.findBySlug(slug)
                .orElseThrow(() -> new CustomException(ErrorCode.FORTUNE_NOT_FOUND));
        UserEntity user = userJpaRepository.findById(draw.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.COMMON_NOT_FOUND));
        SpreadTheme theme = SpreadTheme.fromKey(draw.getSpreadThemeKey());

        List<FortuneCardResponse> cards = draw.getCards().stream()
                .map(drawCard -> buildCardResponse(
                        findCard(drawCard.getCardId()), drawCard.isReversed(), draw.getTopic(),
                        theme.labelAt(drawCard.getPositionIndex())))
                .toList();

        return FortuneDrawResponse.of(draw, cards, draw.getOverallInterpretationText(), user.getNickname());
    }

    /** 마이페이지 "내 기록" 목록. 해석 텍스트는 담지 않아 캐시 조회 없이 가볍게 응답한다. */
    @Transactional(readOnly = true)
    public Page<FortuneSummaryResponse> getMyFortunes(Long userId, Pageable pageable) {
        return fortuneDrawJpaRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toSummary);
    }

    private FortuneSummaryResponse toSummary(FortuneDrawEntity draw) {
        SpreadTheme theme = SpreadTheme.fromKey(draw.getSpreadThemeKey());
        List<FortuneCardResponse> cards = draw.getCards().stream()
                .map(drawCard -> new FortuneCardResponse(
                        theme.labelAt(drawCard.getPositionIndex()),
                        CardBriefResponse.from(findCard(drawCard.getCardId())),
                        drawCard.isReversed(),
                        null))
                .toList();
        return FortuneSummaryResponse.from(draw, cards);
    }

    private FortuneCardResponse buildCardResponse(CardEntity card, boolean reversed, Topic topic, String positionLabel) {
        String interpretation = getOrGenerateCardTopicInterpretation(card, reversed, topic);
        return new FortuneCardResponse(positionLabel, CardBriefResponse.from(card), reversed, interpretation);
    }

    private List<CardEntity> pickThreeDistinctCards() {
        List<CardEntity> cards = new ArrayList<>(cardJpaRepository.findAll());
        Collections.shuffle(cards, ThreadLocalRandom.current());
        return cards.subList(0, SPREAD_SIZE);
    }

    private CardEntity findCard(Short cardId) {
        return cardJpaRepository.findById(cardId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_NOT_FOUND));
    }

    private String generateUniqueSlug() {
        String slug;
        do {
            slug = SlugGenerator.generate();
        } while (fortuneDrawJpaRepository.existsBySlug(slug));
        return slug;
    }

    /** 캐시 조회, 미스면 조합+저장까지 한 번에. */
    private String getOrGenerateCardTopicInterpretation(CardEntity card, boolean reversed, Topic topic) {
        String cachedText = findCachedCardTopicInterpretation(card, reversed, topic);
        if (cachedText != null) {
            return cachedText;
        }
        String text = CardTopicInterpretationComposer.compose(card, reversed, topic);
        cacheCardTopicInterpretation(card, reversed, topic, text);
        return text;
    }

    /** 캐시 조회만(DB 읽기). 미스면 null. */
    private String findCachedCardTopicInterpretation(CardEntity card, boolean reversed, Topic topic) {
        return cardTopicInterpretationJpaRepository.findByCardIdAndReversedAndTopic(card.getId(), reversed, topic)
                .map(CardTopicInterpretationEntity::getInterpretationText)
                .orElse(null);
    }

    /** 새로 생성된 해석을 캐시에 저장(DB 쓰기). */
    private void cacheCardTopicInterpretation(CardEntity card, boolean reversed, Topic topic, String text) {
        try {
            cardTopicInterpretationJpaRepository.save(
                    new CardTopicInterpretationEntity(card.getId(), reversed, topic, text));
        } catch (DataIntegrityViolationException e) {
            // 같은 (카드, 방향, 주제) 조합을 동시에 처음 요청한 다른 트랜잭션과 경합 — 먼저 저장된 캐시가 있으면 무시하고 넘어간다.
            if (findCachedCardTopicInterpretation(card, reversed, topic) == null) {
                throw e;
            }
        }
    }
}
