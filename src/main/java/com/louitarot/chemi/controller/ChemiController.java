package com.louitarot.chemi.controller;

import com.louitarot.chemi.dto.ChemiDrawDetailResponse;
import com.louitarot.chemi.dto.ChemiGuestDrawResponse;
import com.louitarot.chemi.dto.ChemiGuestRequest;
import com.louitarot.chemi.dto.ChemiRankingItemResponse;
import com.louitarot.chemi.service.ChemiService;
import com.louitarot.common.response.ApiMeta;
import com.louitarot.common.response.ApiResponse;
import com.louitarot.common.util.IpHashUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** [[API 명세]]의 케미 뽑기(Chemi Draws) 엔드포인트. 순위 리스트(2차 기능)는 아직 없음. */
@RestController
@RequestMapping("/api/v1/chemi-draws")
public class ChemiController {

    private final ChemiService chemiService;

    public ChemiController(ChemiService chemiService) {
        this.chemiService = chemiService;
    }

    /** 방장(로그인 계정)의 최초 뽑기. 인증 필요. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChemiDrawDetailResponse> createHostDraw(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(chemiService.createHostDraw(userId));
    }

    /** 방장의 공유 링크로 들어온 게스트의 뽑기. 인증 불필요. */
    @PostMapping("/{hostSlug}/guests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChemiGuestDrawResponse> createGuestDraw(
            @PathVariable String hostSlug,
            @Valid @RequestBody ChemiGuestRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipHash = IpHashUtil.hash(clientIp(httpRequest));
        return ApiResponse.success(chemiService.createGuestDraw(hostSlug, request.nickname(), ipHash));
    }

    /** 뽑기 결과 단건 조회. 공개. */
    @GetMapping("/{slug}")
    public ApiResponse<ChemiDrawDetailResponse> getDraw(@PathVariable String slug) {
        return ApiResponse.success(chemiService.getDraw(slug));
    }

    /** 방장 기준 케미 순위 리스트(2차 기능). 공개 — 슬러그를 아는 사람만 접근. */
    @GetMapping("/{hostSlug}/ranking")
    public ApiResponse<List<ChemiRankingItemResponse>> getRanking(
            @PathVariable String hostSlug,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ChemiRankingItemResponse> result = chemiService.getRanking(hostSlug, PageRequest.of(page - 1, size));
        return ApiResponse.success(result.getContent(), ApiMeta.of(page, size, result.getTotalElements()));
    }

    /** ALB/CloudFront 뒤에서 운영되므로(인프라 아키텍처) X-Forwarded-For를 먼저 확인한다. */
    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
