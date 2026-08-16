package com.louitarot.auth.controller;

import com.louitarot.auth.dto.UpdateNicknameRequest;
import com.louitarot.auth.dto.UserSummaryResponse;
import com.louitarot.auth.service.UserService;
import com.louitarot.common.response.ApiMeta;
import com.louitarot.common.response.ApiResponse;
import com.louitarot.fortune.dto.FortuneSummaryResponse;
import com.louitarot.fortune.service.FortuneService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** [[API 명세]]의 마이페이지(users/me) 엔드포인트. 전부 인증 필요 — 별도 permitAll 없이 기본값(anyRequest().authenticated())에 걸린다. */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final FortuneService fortuneService;

    public UserController(UserService userService, FortuneService fortuneService) {
        this.userService = userService;
        this.fortuneService = fortuneService;
    }

    @GetMapping("/me")
    public ApiResponse<UserSummaryResponse> getMe(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(UserSummaryResponse.from(userService.getProfile(userId)));
    }

    @PatchMapping("/me")
    public ApiResponse<UserSummaryResponse> updateMe(
            Authentication authentication, @Valid @RequestBody UpdateNicknameRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(UserSummaryResponse.from(userService.updateNickname(userId, request.nickname())));
    }

    @GetMapping("/me/fortunes")
    public ApiResponse<List<FortuneSummaryResponse>> getMyFortunes(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Page<FortuneSummaryResponse> result = fortuneService.getMyFortunes(userId, PageRequest.of(page - 1, size));
        return ApiResponse.success(result.getContent(), ApiMeta.of(page, size, result.getTotalElements()));
    }
}
