package com.louitarot.ai.adapter.out.nvidia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** NVIDIA(build.nvidia.com)의 OpenAI 호환 chat completions 응답 중 실제로 쓰는 필드만. */
@JsonIgnoreProperties(ignoreUnknown = true)
record NvidiaChatResponse(
        List<Choice> choices
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content) {
    }
}
