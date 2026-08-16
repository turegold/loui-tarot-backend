package com.louitarot.common.util;

import java.security.SecureRandom;

/**
 * 공유 URL(/chemi/{slug}, /fortune/{slug})에 쓰는 짧은 식별자를 생성한다 ([[DB 스키마]]).
 * PK를 그대로 노출하지 않아 링크가 짧고 순번 추측도 방지한다는 설계 의도.
 *
 * 0/O, 1/l/I처럼 헷갈리는 문자는 제외한 62자 알파벳에서 10자를 뽑는다 — 62^10 조합이라
 * 충돌 확률은 실무적으로 무시 가능하지만, 그래도 DB의 UNIQUE 제약이 최종 방어선이다
 * (호출부가 저장 전에 existsBySlug로 한 번 더 확인하는 걸 권장).
 */
public final class SlugGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";
    private static final int LENGTH = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SlugGenerator() {
    }

    public static String generate() {
        StringBuilder slug = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            slug.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return slug.toString();
    }
}
