package com.louitarot.ai.adapter.out.nvidia;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** NVIDIA(build.nvidia.com)의 OpenAI 호환 chat completions 요청 바디. */
record NvidiaChatRequest(
        List<Message> messages,
        String model,
        @JsonProperty("chat_template_kwargs") ChatTemplateKwargs chatTemplateKwargs,
        @JsonProperty("max_tokens") int maxTokens,
        boolean stream,
        double temperature,
        @JsonProperty("top_p") double topP
) {

    record Message(String role, String content) {
    }

    /** enable_thinking을 꺼서, Gemma가 추론 과정을 출력에 섞지 않고 결과 텍스트만 바로 내도록 한다. */
    record ChatTemplateKwargs(@JsonProperty("enable_thinking") boolean enableThinking) {
    }
}
