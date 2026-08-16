package com.louitarot.fortune.service;

import com.louitarot.ai.application.port.out.AiInterpretationPort;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 개인 카드 뽑기(3장 스프레드) 오케스트레이션 — 레이어드. 케미 뽑기와 동일하게 카드/유저
 * 리포지토리를 직접 주입받고, AI 호출만 AiInterpretationPort(헥사고날)로 위임한다.
 */
@Service
public class FortuneService {

    private static final int SPREAD_SIZE = 3;

    private final FortuneDrawJpaRepository fortuneDrawJpaRepository;
    private final CardJpaRepository cardJpaRepository;
    private final CardTopicInterpretationJpaRepository cardTopicInterpretationJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final AiInterpretationPort aiInterpretationPort;

    public FortuneService(
            FortuneDrawJpaRepository fortuneDrawJpaRepository,
            CardJpaRepository cardJpaRepository,
            CardTopicInterpretationJpaRepository cardTopicInterpretationJpaRepository,
            UserJpaRepository userJpaRepository,
            AiInterpretationPort aiInterpretationPort
    ) {
        this.fortuneDrawJpaRepository = fortuneDrawJpaRepository;
        this.cardJpaRepository = cardJpaRepository;
        this.cardTopicInterpretationJpaRepository = cardTopicInterpretationJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.aiInterpretationPort = aiInterpretationPort;
    }

    /**
     * 3장을 한 번에 뽑아 카드별 해석 + 종합 해석까지 함께 생성한다.
     *
     * AI 호출 4개(카드 3장 + 종합)를 순차로 하면 호출 하나당 수 초~십수 초가 걸려 응답이
     * 최대 4배로 느려진다(실측 47초). 넷 다 서로 독립적인 호출이라 병렬로 실행해서
     * 가장 느린 호출 하나만큼의 시간으로 줄인다 — 단, DB 접근(캐시 조회/저장)은 트랜잭션이
     * 묶인 메인 스레드에서만 하고, 병렬 스레드는 순수 AI 호출(네트워크 I/O)만 하도록 분리했다.
     */
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

        // 1) 캐시 조회부터 메인 스레드에서 순차로 끝낸다 — 캐시 히트면 AI 호출 자체가 필요 없다.
        List<String> cached = new ArrayList<>(SPREAD_SIZE);
        for (int i = 0; i < SPREAD_SIZE; i++) {
            cached.add(findCachedCardTopicInterpretation(pickedCards.get(i), reversedFlags.get(i), topic));
        }

        // 2) 캐시 미스인 카드 + 종합 해석만 AI 호출이 필요하다 — 서로 독립적이니 병렬로 실행한다.
        List<CompletableFuture<String>> cardTextFutures = new ArrayList<>(SPREAD_SIZE);
        for (int i = 0; i < SPREAD_SIZE; i++) {
            String cachedText = cached.get(i);
            if (cachedText != null) {
                cardTextFutures.add(CompletableFuture.completedFuture(cachedText));
                continue;
            }
            CardEntity card = pickedCards.get(i);
            boolean reversed = reversedFlags.get(i);
            cardTextFutures.add(CompletableFuture.supplyAsync(() -> generateCardTopicInterpretationText(card, reversed, topic)));
        }
        CompletableFuture<String> overallFuture = CompletableFuture.supplyAsync(
                () -> generateOverallInterpretation(topic, theme, pickedCards, reversedFlags));

        List<String> cardTexts = cardTextFutures.stream().map(CompletableFuture::join).toList();
        String overallInterpretation = overallFuture.join();

        // 3) 새로 생성된 것만 메인 스레드로 돌아와 캐시에 저장한다.
        for (int i = 0; i < SPREAD_SIZE; i++) {
            if (cached.get(i) == null) {
                cacheCardTopicInterpretation(pickedCards.get(i), reversedFlags.get(i), topic, cardTexts.get(i));
            }
        }

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

    /** 마이페이지 "내 기록" 목록. 해석 텍스트는 담지 않아 AI/캐시 조회 없이 가볍게 응답한다. */
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

    /** 순차 경로(getDraw)용 — 캐시 조회, 미스면 생성+저장까지 한 번에. */
    private String getOrGenerateCardTopicInterpretation(CardEntity card, boolean reversed, Topic topic) {
        String cachedText = findCachedCardTopicInterpretation(card, reversed, topic);
        if (cachedText != null) {
            return cachedText;
        }
        String text = generateCardTopicInterpretationText(card, reversed, topic);
        cacheCardTopicInterpretation(card, reversed, topic, text);
        return text;
    }

    /** 캐시 조회만(DB 읽기). 미스면 null — AI 호출은 하지 않는다. */
    private String findCachedCardTopicInterpretation(CardEntity card, boolean reversed, Topic topic) {
        return cardTopicInterpretationJpaRepository.findByCardIdAndReversedAndTopic(card.getId(), reversed, topic)
                .map(CardTopicInterpretationEntity::getInterpretationText)
                .orElse(null);
    }

    /** 순수 AI 호출(네트워크 I/O)만. DB에 손대지 않아 병렬 스레드에서 안전하게 실행할 수 있다. */
    private String generateCardTopicInterpretationText(CardEntity card, boolean reversed, Topic topic) {
        String prompt = "당신은 타로 카드 해석가입니다. '%s' 카드가 %s으로 나왔을 때, '%s' 관점에서 2~3문장으로 해석해주세요."
                .formatted(card.getNameKr(), reversed ? "역방향" : "정방향", topic.label());
        return aiInterpretationPort.generate(prompt);
    }

    /** 새로 생성된 해석을 캐시에 저장(DB 쓰기) — 반드시 메인(트랜잭션) 스레드에서 호출해야 한다. */
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

    /** 3장 조합의 경우의 수가 너무 많아 캐싱하지 않고 매 요청마다 생성한다 ([[AI 해석 캐싱 전략]]). */
    private String generateOverallInterpretation(
            Topic topic, SpreadTheme theme, List<CardEntity> cards, List<Boolean> reversedFlags) {
        StringBuilder cardLines = new StringBuilder();
        for (int i = 0; i < SPREAD_SIZE; i++) {
            cardLines.append("%d. %s: %s(%s)%n".formatted(
                    i + 1, theme.labelAt(i), cards.get(i).getNameKr(),
                    reversedFlags.get(i) ? "역방향" : "정방향"));
        }
        String prompt = """
                당신은 타로 카드 해석가입니다. '%s' 주제로 3장의 카드를 뽑았습니다:
                %s
                이 3장의 흐름을 종합해서 2~3문장으로 해석해주세요.
                """.formatted(topic.label(), cardLines);
        return aiInterpretationPort.generate(prompt);
    }
}
