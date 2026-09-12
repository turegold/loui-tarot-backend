package com.louitarot.card.domain;

/** 카드 한 장(정/역)의 재사용 가능한 해석 조각 — AI 호출 없이 조합하는 템플릿 해석의 재료. */
public record CardEssence(String keyword, CardLeaning leaning, String advice) {
}
