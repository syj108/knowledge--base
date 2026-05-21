package com.example.precip.pipeline;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.kb.IndexGenerator;
import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.kb.StateService;
import com.example.precip.link.*;
import com.example.precip.model.AgentState;
import com.example.precip.model.GraphData;
import com.example.precip.model.KnowledgePage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PostProcessServiceTest {

    @TempDir
    Path tempDir;

    private PostProcessService postProcessService;
    private KnowledgeBaseService kbService;
    private StateService stateService;

    @BeforeEach
    void setUp() throws Exception {
        // 构建知识库骨架
        KnowledgeBaseConfig config = new KnowledgeBaseConfig() {
            @Override
            public Path templatesDir() { return tempDir.resolve("templates"); }
            @Override
            public Path pagesDir() { return tempDir.resolve("pages"); }
            @Override
            public Path sourcesDir() { return tempDir.resolve("sources"); }
            @Override
            public Path graphFile() { return tempDir.resolve("graph.json"); }
            @Override
            public Path stateFile() { return tempDir.resolve("state.json"); }
            @Override
            public Path indexFile() { return tempDir.resolve("index.md"); }
        };

        Files.createDirectories(config.pagesDir());
        Files.createDirectories(config.templatesDir());
        Files.createDirectories(config.sourcesDir());

        ObjectMapper om = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        Files.writeString(config.graphFile(), om.writeValueAsString(new GraphData()));
        Files.writeString(config.stateFile(), om.writeValueAsString(new AgentState()));

        kbService = new KnowledgeBaseService(config);
        stateService = new StateService(kbService);

        // 先添加源记录
        stateService.addSource("src-001", "测试源", "FREE_TEXT", "sha256:abc", "/tmp/mock");

        WikiLinkParser wikiLinkParser = new WikiLinkParser();
        LinkInjector linkInjector = new LinkInjector(kbService, wikiLinkParser);
        DeadLinkCleaner deadLinkCleaner = new DeadLinkCleaner(kbService, wikiLinkParser);
        LinkGraph linkGraph = new LinkGraph(kbService, wikiLinkParser);

        // IndexGenerator 需要 LlmClient，用异常回退方式跳过 LLM 调用
        IndexGenerator indexGenerator = new IndexGenerator(kbService, null, null) {
            @Override
            public void regenerate() throws java.io.IOException {
                // 简化版：直接生成静态索引
                kbService.writeIndex("# 知识库索引\n\n测试索引内容\n");
            }
        };

        postProcessService = new PostProcessService(
                linkInjector, deadLinkCleaner, kbService, linkGraph,
                indexGenerator, stateService);
    }

    @Test
    void shouldWritePagesAndUpdateState() throws Exception {
        KnowledgePage page = new KnowledgePage();
        page.setSlug("design/rest-api");
        page.setTitle("REST API 设计");
        page.setCategory("design");
        page.setContent("# REST API 设计\n\n这是 REST API 设计文档。\n");
        page.setSourceId("src-001");

        postProcessService.process(List.of(page), "src-001");

        // 验证页面文件已写入
        assertTrue(kbService.pageExists("design/rest-api"), "页面应已写入");
        String content = kbService.readPage("design/rest-api");
        assertTrue(content.contains("REST API 设计"), "内容应正确");

        // 验证 state.json 更新
        AgentState.SourceRecord record = stateService.getSourceRecord("src-001");
        assertEquals("completed", record.getStatus());
        assertTrue(record.getGeneratedPages().contains("design/rest-api"));

        // 验证 graph.json 更新
        GraphData graph = kbService.readGraphData();
        assertTrue(graph.getNodes().containsKey("design/rest-api"), "图谱应包含新节点");

        // 验证 index.md 已创建
        assertTrue(Files.exists(tempDir.resolve("index.md")), "index.md 应已生成");
    }

    @Test
    void shouldHandleMultiplePages() throws Exception {
        KnowledgePage page1 = new KnowledgePage();
        page1.setSlug("design/page-one");
        page1.setTitle("页面一");
        page1.setCategory("design");
        page1.setContent("# 页面一\n\n内容一。\n");
        page1.setSourceId("src-001");

        KnowledgePage page2 = new KnowledgePage();
        page2.setSlug("guide/page-two");
        page2.setTitle("页面二");
        page2.setCategory("guide");
        page2.setContent("# 页面二\n\n内容二，提到页面一。\n");
        page2.setSourceId("src-001");

        postProcessService.process(List.of(page1, page2), "src-001");

        assertTrue(kbService.pageExists("design/page-one"));
        assertTrue(kbService.pageExists("guide/page-two"));

        AgentState.SourceRecord record = stateService.getSourceRecord("src-001");
        assertEquals(2, record.getGeneratedPages().size());
    }
}
