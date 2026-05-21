package com.example.precip.llm;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.example.precip.config.LlmConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Supplier;

/**
 * DashScope SDK 封装，提供通用文本生成和结构化 JSON 输出能力。
 */
@Service
public class LlmClient {

    private static final Logger log = LoggerFactory.getLogger(LlmClient.class);

    private final LlmConfig config;
    private final ObjectMapper objectMapper;
    private final GenerationCaller caller;

    /**
     * 抽象 DashScope 调用，便于测试时替换。
     */
    @FunctionalInterface
    public interface GenerationCaller {
        GenerationResult call(GenerationParam param) throws Exception;
    }

    @Autowired
    public LlmClient(LlmConfig config) {
        this(config, new Generation()::call);
    }

    public LlmClient(LlmConfig config, GenerationCaller caller) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.caller = caller;
    }

    public String chat(String systemPrompt, String userMessage) {
        return withRetry(() -> doChat(systemPrompt, userMessage));
    }

    public <T> T chatForJson(String systemPrompt, String userMessage, Class<T> type) {
        String response = chat(systemPrompt, userMessage);
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.error("JSON 解析失败，原始响应: {}", response, e);
            throw new RuntimeException("LLM 返回内容无法解析为 " + type.getSimpleName(), e);
        }
    }

    public <T> T chatForJson(String systemPrompt, String userMessage, TypeReference<T> typeRef) {
        String response = chat(systemPrompt, userMessage);
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("JSON 解析失败，原始响应: {}", response, e);
            throw new RuntimeException("LLM 返回内容无法解析为目标类型", e);
        }
    }

    private String doChat(String systemPrompt, String userMessage) {
        try {
            Message systemMsg = Message.builder()
                    .role(Role.SYSTEM.getValue())
                    .content(systemPrompt)
                    .build();
            Message userMsg = Message.builder()
                    .role(Role.USER.getValue())
                    .content(userMessage)
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .apiKey(config.getApiKey())
                    .model(config.getModel())
                    .messages(List.of(systemMsg, userMsg))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .temperature(config.getTemperature())
                    .build();

            GenerationResult result = caller.call(param);
            return result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            throw new RuntimeException("DashScope 调用失败: " + e.getMessage(), e);
        }
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private <T> T withRetry(Supplier<T> call) {
        int maxAttempts = config.getRetryMaxAttempts();
        long backoffMs = config.getRetryBackoffMs();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    log.error("LLM 调用在 {} 次重试后仍然失败", maxAttempts, e);
                    throw e;
                }
                log.warn("LLM 调用失败（第 {}/{} 次），{}ms 后重试: {}",
                        attempt, maxAttempts, backoffMs, e.getMessage());
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
                backoffMs *= 2;
            }
        }
        throw new IllegalStateException("不应到达此处");
    }
}
