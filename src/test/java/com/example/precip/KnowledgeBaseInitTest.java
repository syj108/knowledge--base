package com.example.precip;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.model.AgentState;
import com.example.precip.model.GraphData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KnowledgeBaseInitTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("precip.kb.base-dir", () -> tempDir.toString());
    }

    @Autowired
    private KnowledgeBaseConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void shouldCreateKnowledgeBaseSkeleton() {
        // 验证目录结构
        assertTrue(Files.isDirectory(config.templatesDir()), "templates/ 目录应存在");
        assertTrue(Files.isDirectory(config.pagesDir()), "pages/ 目录应存在");
        assertTrue(Files.isDirectory(config.sourcesDir()), "sources/ 目录应存在");

        // 验证 JSON 文件
        assertTrue(Files.exists(config.graphFile()), "graph.json 应存在");
        assertTrue(Files.exists(config.stateFile()), "state.json 应存在");
    }

    @Test
    void shouldInitializeEmptyGraphJson() throws Exception {
        GraphData graph = objectMapper.readValue(config.graphFile().toFile(), GraphData.class);
        assertEquals(1, graph.getVersion());
        assertTrue(graph.getNodes().isEmpty());
        assertTrue(graph.getEdges().isEmpty());
    }

    @Test
    void shouldInitializeEmptyStateJson() throws Exception {
        AgentState state = objectMapper.readValue(config.stateFile().toFile(), AgentState.class);
        assertEquals(1, state.getKbVersion());
        assertTrue(state.getSources().isEmpty());
        assertNotNull(state.getCreatedAt());
    }

    @Test
    void shouldCopyDefaultTemplate() throws Exception {
        Path template = config.templatesDir().resolve("knowledgeBaseOutput.md");
        assertTrue(Files.exists(template), "默认模板应被复制到 templates/");
        String content = Files.readString(template);
        assertTrue(content.contains("产品对外文档"), "模板内容应包含预期的 H1 标题");
    }

    @Test
    void shouldInitGitRepository() {
        assertTrue(Files.isDirectory(config.getBaseDir().resolve(".git")),
                "知识库目录应初始化为 Git 仓库");
    }
}
