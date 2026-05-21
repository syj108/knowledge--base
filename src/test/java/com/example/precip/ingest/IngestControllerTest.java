package com.example.precip.ingest;

import com.example.precip.ingest.parser.SourceParser;
import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.kb.StateService;
import com.example.precip.model.SourceContent;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 直接实例化 Controller 进行测试，完全避免 Mockito 对 JDK 25 final 类的限制。
 */
class IngestControllerTest {

    @Test
    void shouldAcceptTextSource() throws Exception {
        // 构造固定返回值的 mock 实现
        SourceContent mockContent = new SourceContent();
        mockContent.setSourceId("test-id-123");
        mockContent.setType(SourceContent.SourceType.FREE_TEXT);
        mockContent.setTitle("测试文本");
        mockContent.setRawText("内容");
        mockContent.setContentHash("sha256:abc");

        boolean[] stateAdded = {false};
        boolean[] eventPublished = {false};

        var textParser = new com.example.precip.ingest.parser.TextParser() {
            @Override
            public SourceContent parse(Object input, String title) {
                return mockContent;
            }
        };

        var stateService = new StateService(null) {
            @Override
            public void addSource(String sourceId, String title, String type,
                                  String contentHash, String filePath) {
                assertEquals("test-id-123", sourceId);
                assertEquals("测试文本", title);
                stateAdded[0] = true;
            }
        };

        var kbService = new KnowledgeBaseService(null) {
            @Override
            public Path saveSourceFile(String sourceId, String fileName, byte[] content) {
                return Path.of("/tmp/mock");
            }
            @Override
            public Path getSourceDir(String sourceId) {
                return Path.of("/tmp/mock/" + sourceId);
            }
        };

        ApplicationEventPublisher publisher = event -> eventPublished[0] = true;

        var controller = new IngestController(
                null, textParser, null, new SourcePreprocessor(),
                kbService, stateService, publisher);

        var request = new IngestRequest.TextIngestRequest("测试文本", "这是测试内容", "zh");
        var response = controller.ingestText(request);

        assertEquals(202, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("test-id-123", response.getBody().sourceId());
        assertEquals("parsed", response.getBody().status());
        assertTrue(stateAdded[0], "state.json 应已更新");
        assertTrue(eventPublished[0], "事件应已发布");
    }
}
