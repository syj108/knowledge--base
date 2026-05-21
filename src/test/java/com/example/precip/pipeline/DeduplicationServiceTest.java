package com.example.precip.pipeline;

import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.link.LinkGraph;
import com.example.precip.llm.LlmClient;
import com.example.precip.llm.PromptBuilder;
import com.example.precip.model.KnowledgePage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeduplicationServiceTest {

    @Test
    void shouldMarkNewPageWhenSlugNotExists() throws Exception {
        // LinkGraph.nodeExists 总返回 false
        LinkGraph linkGraph = new LinkGraph(null, null) {
            @Override
            public boolean nodeExists(String slug) {
                return false;
            }
        };

        DeduplicationService service = new DeduplicationService(
                null, linkGraph, null, null);

        KnowledgePage page = new KnowledgePage();
        page.setSlug("design/new-topic");
        page.setTitle("新主题");
        page.setContent("内容");

        List<KnowledgePage> result = service.deduplicate(List.of(page));

        assertEquals(1, result.size());
        assertTrue(result.get(0).isNew(), "新 slug 应标记为 isNew=true");
    }

    @Test
    void shouldHandleDeduplicationFailureGracefully() throws Exception {
        // LinkGraph.nodeExists 抛异常
        LinkGraph linkGraph = new LinkGraph(null, null) {
            @Override
            public boolean nodeExists(String slug) throws java.io.IOException {
                throw new RuntimeException("模拟异常");
            }
        };

        DeduplicationService service = new DeduplicationService(
                null, linkGraph, null, null);

        KnowledgePage page = new KnowledgePage();
        page.setSlug("design/topic");
        page.setTitle("主题");
        page.setContent("内容");

        List<KnowledgePage> result = service.deduplicate(List.of(page));

        assertEquals(1, result.size());
        assertTrue(result.get(0).isNew(), "去重失败时应回退为新页面");
    }

    @Test
    void shouldProcessMultiplePages() throws Exception {
        LinkGraph linkGraph = new LinkGraph(null, null) {
            @Override
            public boolean nodeExists(String slug) {
                return false;
            }
        };

        DeduplicationService service = new DeduplicationService(
                null, linkGraph, null, null);

        KnowledgePage page1 = new KnowledgePage();
        page1.setSlug("a/first");
        page1.setTitle("第一");
        page1.setContent("内容1");

        KnowledgePage page2 = new KnowledgePage();
        page2.setSlug("b/second");
        page2.setTitle("第二");
        page2.setContent("内容2");

        List<KnowledgePage> result = service.deduplicate(List.of(page1, page2));
        assertEquals(2, result.size());
    }
}
