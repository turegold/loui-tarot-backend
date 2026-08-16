package com.louitarot.fortune.controller;

import com.louitarot.common.response.ApiResponse;
import com.louitarot.fortune.dto.FortuneCreateRequest;
import com.louitarot.fortune.dto.FortuneDrawResponse;
import com.louitarot.fortune.service.FortuneService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** [[API 명세]]의 개인 카드 뽑기(Fortunes) 뽑기/조회 엔드포인트. 마이페이지 관련 엔드포인트는 별도 작업. */
@RestController
@RequestMapping("/api/v1/fortunes")
public class FortuneController {

    private final FortuneService fortuneService;

    public FortuneController(FortuneService fortuneService) {
        this.fortuneService = fortuneService;
    }

    /** 3장을 한 번에 뽑아 카드별 해석 + 종합 해석까지 반환. 인증 필요. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FortuneDrawResponse> createDraw(
            Authentication authentication, @Valid @RequestBody FortuneCreateRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(fortuneService.createDraw(userId, request.topic()));
    }

    /** 결과 공유 링크 조회. 공개. */
    @GetMapping("/{slug}")
    public ApiResponse<FortuneDrawResponse> getDraw(@PathVariable String slug) {
        return ApiResponse.success(fortuneService.getDraw(slug));
    }
}
