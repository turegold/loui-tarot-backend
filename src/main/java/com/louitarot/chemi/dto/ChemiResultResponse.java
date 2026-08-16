package com.louitarot.chemi.dto;

/** host-guest 케미 결과(점수 + 해석). [[케미 점수 산출 로직]]으로 계산된 점수. */
public record ChemiResultResponse(short score, String interpretation) {
}
