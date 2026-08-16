package com.louitarot.chemi.dto;

/** [[API 명세]]의 {@code POST /chemi-draws/{hostSlug}/guests} 응답. */
public record ChemiGuestDrawResponse(
        ChemiDrawSummaryResponse guestDraw,
        ChemiDrawSummaryResponse hostDraw,
        ChemiResultResponse chemi
) {
}
