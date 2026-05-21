package com.example.precip.pipeline;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.kb.StateService;
import com.example.precip.llm.LlmClient;
import com.example.precip.model.AgentState;
import com.example.precip.model.GraphData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IngestionPipelineIntegrationTest {

    static Path tempDir;

    static {
        try {
            tempDir = Files.createTempDirectory("precip-test-");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("precip.kb.base-dir", () -> tempDir.toString());
    }

    @TestConfiguration
    static class MockLlmConfig {
        @Bean
        @Primary
        public LlmClient mockLlmClient() {
            return new LlmClient(
                    new com.example.precip.config.LlmConfig(),
                    param -> {
                        String userContent = param.getMessages().stream()
                                .filter(m -> "user".equals(m.getRole()))
                                .map(m -> (String) m.getContent())
                                .findFirst().orElse("");

                        String systemContent = param.getMessages().stream()
                                .filter(m -> "system".equals(m.getRole()))
                                .map(m -> (String) m.getContent())
                                .findFirst().orElse("");

                        String response;
                        if (systemContent.contains("JSON 数组") || systemContent.contains("候选")) {
                            // Pass 0: 返回候选列表
                            response = """
                                    [{"name":"测试知识文档","category":"knowledge-base-output","slug":"knowledge-base-output/test-doc","templateFile":"knowledgeBaseOutput.md","description":"测试用的知识文档"}]
                                    """;
                        } else if (systemContent.contains("知识库管理员")) {
                            // 索引简介
                            response = "这是一个测试知识库，包含测试文档。";
                        } else {
                            // Pass 1: 返回生成的文档
                            response = """
                                    # 测试知识文档

                                    ## 概述

                                    这是通过集成测试自动生成的知识文档。

                                    ## 详细内容

                                    测试内容段落。
                                    """;
                        }
                        return buildGenerationResult(response);
                    }
            );
        }

        private com.alibaba.dashscope.aigc.generation.GenerationResult buildGenerationResult(String content) {
            try {
                var resultClass = com.alibaba.dashscope.aigc.generation.GenerationResult.class;
                var constructor = resultClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                var result = constructor.newInstance();

                var output = new com.alibaba.dashscope.aigc.generation.GenerationOutput();
                var choice = output.new Choice();

                var message = com.alibaba.dashscope.common.Message.builder()
                        .role("assistant")
                        .content(content)
                        .build();
                choice.setMessage(message);
                output.setChoices(java.util.List.of(choice));
                result.setOutput(output);
                return result;
            } catch (Exception e) {
                throw new RuntimeException("构建模拟 GenerationResult 失败", e);
            }
        }
    }

    @Autowired
    private IngestionPipeline pipeline;

    @Autowired
    private KnowledgeBaseService kbService;

    @Autowired
    private StateService stateService;

    @Autowired
    private KnowledgeBaseConfig config;

    @Test
    void fullPipeline_textSource_producesKnowledgePage() throws Exception {
        // 1. 模拟源文档摄取：创建源记录和源文件
        String sourceId = "integration-test-001";

        stateService.addSource(sourceId, "集成测试文档", "FREE_TEXT",
                "sha256:test", "/tmp/test");

        // 创建源文件目录和内容
        Path sourceDir = config.sourcesDir().resolve(sourceId);
        Files.createDirectories(sourceDir);
        Files.writeString(sourceDir.resolve("content.txt"),
                "这是集成测试的源文本内容。包含关于 REST API 设计和系统架构的知识。",
                StandardCharsets.UTF_8);

        // 2. 执行管道（同步调用）
        pipeline.execute(sourceId);

        // 3. 验证 pages/ 下生成了文件
        var slugs = kbService.allPageSlugs();
        assertFalse(slugs.isEmpty(), "pages/ 目录下应有生成的 .md 文件");

        // 4. 验证 graph.json 有节点
        GraphData graph = kbService.readGraphData();
        assertFalse(graph.getNodes().isEmpty(), "graph.json 应有节点");

        // 5. 验证 state.json 源状态 = completed
        AgentState.SourceRecord record = stateService.getSourceRecord(sourceId);
        assertNotNull(record);
        assertEquals("completed", record.getStatus(), "源状态应为 completed");
        assertFalse(record.getGeneratedPages().isEmpty(), "应有生成的页面列表");

        // 6. 验证 index.md 已创建
        assertTrue(Files.exists(config.indexFile()), "index.md 应已生成");
        String indexContent = Files.readString(config.indexFile());
        assertTrue(indexContent.contains("知识库"), "索引应包含标题");

        // 7. 验证生成的页面内容
        String firstSlug = slugs.get(0);
        String pageContent = kbService.readPage(firstSlug);
        assertNotNull(pageContent);
        assertFalse(pageContent.isBlank(), "页面内容不应为空");
    }
}
