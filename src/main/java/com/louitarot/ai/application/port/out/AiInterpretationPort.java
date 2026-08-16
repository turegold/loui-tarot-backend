package com.louitarot.ai.application.port.out;

/**
 * AI로 타로 해석 텍스트를 생성하는 흐름의 추상화.
 *
 * 헥사고날을 여기 적용한 이유(백엔드 설계 원칙 1번): AI 공급자의 SDK/HTTP 요청 형태 같은
 * 세부사항이 케미/개인 카드 뽑기 서비스 코드로 새어 들어오지 않게 막고, 실제 API를 호출하지
 * 않고도 가짜 구현체로 그 흐름을 테스트할 수 있게 한다.
 *
 * 프롬프트를 무엇으로 구성할지(카드/방향/주제/점수 등을 어떻게 문장으로 엮을지)는 이 포트의
 * 책임이 아니다 — 그건 프롬프트를 만드는 쪽(chemi/fortune 서비스)의 도메인 지식이고, 이 포트는
 * "완성된 프롬프트를 받아 생성된 텍스트를 돌려준다"는 얇은 계약만 진다.
 */
public interface AiInterpretationPort {

    /**
     * 프롬프트를 AI에 보내 해석 텍스트를 생성한다.
     * 실패(타임아웃, API 오류, 빈 응답 등)하면 CustomException(AI_INTERPRETATION_FAILED).
     */
    String generate(String prompt);
}
