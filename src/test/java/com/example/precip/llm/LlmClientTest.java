package com.example.precip.llm;

import com.alibaba.dashscope.aigc.generation.GenerationOutput;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.example.precip.config.LlmConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class LlmClientTest {

    private LlmConfig config;

    @BeforeEach
    void setUp() {
        config = new LlmConfig();
        config.setApiKey("test-key");
        config.setModel("qwen-plus");
        config.setRetryMaxAttempts(3);
        config.setRetryBackoffMs(10);
    }

    private GenerationResult buildResult(String content) throws Exception {
        Message msg = Message.builder()
                .role("assistant")
                .content(content)
                .build();
        GenerationOutput output = new GenerationOutput();
        GenerationOutput.Choice choice = output.new Choice();
        choice.setMessage(msg);
        output.setChoices(List.of(choice));

        var constructor = GenerationResult.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        GenerationResult result = constructor.newInstance();
        result.setOutput(output);
        return result;
    }

    private LlmClient clientReturning(String response) {
        return new LlmClient(config, param -> buildResult(response));
    }

    @Test
    void shouldReturnChatResponse() {
        String result = clientReturning("这是一段生成的文本内容").chat("system", "user");
        assertEquals("这是一段生成的文本内容", result);
    }

    @Test
    void shouldParseJsonResponse() {
        TestRecord result = clientReturning("{\"name\": \"测试\", \"value\": 42}")
                .chatForJson("system", "user", TestRecord.class);
        assertEquals("测试", result.name);
        assertEquals(42, result.value);
    }

    @Test
    void shouldHandleMarkdownWrappedJson() {
        TestRecord result = clientReturning("```json\n{\"name\": \"wrapped\", \"value\": 1}\n```")
                .chatForJson("system", "user", TestRecord.class);
        assertEquals("wrapped", result.name);
    }

    @Test
    void shouldRetryOnFailureThenSucceed() {
        AtomicInteger count = new AtomicInteger(0);
        LlmClient client = new LlmClient(config, param -> {
            if (count.incrementAndGet() == 1) {
                throw new RuntimeException("临时错误");
            }
            return buildResult("重试成功");
        });

        assertEquals("重试成功", client.chat("system", "user"));
        assertEquals(2, count.get());
    }

    @Test
    void shouldThrowAfterMaxRetries() {
        AtomicInteger count = new AtomicInteger(0);
        LlmClient client = new LlmClient(config, param -> {
            count.incrementAndGet();
            throw new RuntimeException("持续错误");
        });

        assertThrows(RuntimeException.class, () -> client.chat("system", "user"));
        assertEquals(3, count.get());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TestRecord {
        public String name;
        public int value;
    }
}
