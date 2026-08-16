package com.louitarot.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 원본 IP는 저장하지 않고(개인정보 최소 수집, [[06 개인정보 및 인증 정책]]) SHA-256 해시만
 * 남긴다 — Rate Limiting/어뷰징 방지용으로만 쓰고 개인 식별 목적이 아니라서 되돌릴 필요가 없다.
 */
public final class IpHashUtil {

    private IpHashUtil() {
    }

    public static String hash(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM이 기본 제공하는 알고리즘이라 실제로는 발생하지 않는다.
            throw new IllegalStateException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
