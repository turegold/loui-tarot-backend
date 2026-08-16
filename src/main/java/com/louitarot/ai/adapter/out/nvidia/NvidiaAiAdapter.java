package com.louitarot.ai.adapter.out.nvidia;

import com.louitarot.ai.application.port.out.AiInterpretationPort;
import com.louitarot.common.exception.CustomException;
import com.louitarot.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * AiInterpretationPort의 실제 구현체 — NVIDIA(build.nvidia.com)의 OpenAI 호환 API로
 * Gemma 4 31B(google/gemma-4-31b-it)를 호출한다. 실제 타로 해석 프롬프트로 한국어 품질을
 * 확인한 뒤 채택한 모델([[백엔드 설계 원칙]] 참고).
 */
@Component
public class NvidiaAiAdapter implements AiInterpretationPort {

    private static final Logger log = LoggerFactory.getLogger(NvidiaAiAdapter.class);
    private static final String CHAT_COMPLETIONS_URI = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final int MAX_TOKENS = 1024;
    private static final double TEMPERATURE = 1.0;
    private static final double TOP_P = 0.95;

    private final RestClient restClient = RestClient.create();
    private final String apiKey;
    private final String model;

    public NvidiaAiAdapter(
            @Value("${ai.nvidia.api-key}") String apiKey,
            @Value("${ai.nvidia.model}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generate(String prompt) {
        NvidiaChatRequest request = new NvidiaChatRequest(
                List.of(new NvidiaChatRequest.Message("user", prompt)),
                model,
                new NvidiaChatRequest.ChatTemplateKwargs(false),
                MAX_TOKENS,
                false,
                TEMPERATURE,
                TOP_P);

        try {
            NvidiaChatResponse response = restClient.post()
                    .uri(CHAT_COMPLETIONS_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(NvidiaChatResponse.class);

            String content = extractContent(response);
            if (content == null || content.isBlank()) {
                throw new CustomException(ErrorCode.AI_INTERPRETATION_FAILED);
            }
            return content;
        } catch (RestClientException e) {
            log.warn("AI 해석 생성 실패", e);
            throw new CustomException(ErrorCode.AI_INTERPRETATION_FAILED);
        }
    }

    private String extractContent(NvidiaChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return null;
        }
        return response.choices().get(0).message().content();
    }
}
