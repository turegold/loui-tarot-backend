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
import com.louitarot.fortune.entity.FortuneDrawCardEntity;
import com.louitarot.fortune.entity.FortuneDrawEntity;
import com.louitarot.fortune.repository.FortuneDrawJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /** 3장을 한 번에 뽑아 카드별 해석 + 종합 해석까지 함께 생성한다. */
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

        List<FortuneCardResponse> cardResponses = new ArrayList<>(SPREAD_SIZE);
        for (int i = 0; i < SPREAD_SIZE; i++) {
            cardResponses.add(buildCardResponse(pickedCards.get(i), reversedFlags.get(i), topic, theme.labelAt(i)));
        }
        String overallInterpretation = generateOverallInterpretation(topic, theme, pickedCards, reversedFlags);

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

    private String getOrGenerateCardTopicInterpretation(CardEntity card, boolean reversed, Topic topic) {
        return cardTopicInterpretationJpaRepository.findByCardIdAndReversedAndTopic(card.getId(), reversed, topic)
                .map(CardTopicInterpretationEntity::getInterpretationText)
                .orElseGet(() -> generateAndCacheCardTopicInterpretation(card, reversed, topic));
    }

    private String generateAndCacheCardTopicInterpretation(CardEntity card, boolean reversed, Topic topic) {
        String prompt = "당신은 타로 카드 해석가입니다. '%s' 카드가 %s으로 나왔을 때, '%s' 관점에서 2~3문장으로 해석해주세요."
                .formatted(card.getNameKr(), reversed ? "역방향" : "정방향", topic.label());
        String text = aiInterpretationPort.generate(prompt);
        try {
            cardTopicInterpretationJpaRepository.save(
                    new CardTopicInterpretationEntity(card.getId(), reversed, topic, text));
            return text;
        } catch (DataIntegrityViolationException e) {
            // 같은 (카드, 방향, 주제) 조합을 동시에 처음 요청한 다른 트랜잭션과 경합 — 먼저 저장된 캐시를 그대로 쓴다.
            return cardTopicInterpretationJpaRepository.findByCardIdAndReversedAndTopic(card.getId(), reversed, topic)
                    .map(CardTopicInterpretationEntity::getInterpretationText)
                    .orElseThrow(() -> e);
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
