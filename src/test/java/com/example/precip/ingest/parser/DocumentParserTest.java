package com.example.precip.ingest.parser;

import com.example.precip.model.SourceContent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {

    private final DocumentParser parser = new DocumentParser();

    @Test
    void shouldParsePlainText() throws Exception {
        String text = "这是一段纯文本内容，用于测试文档解析器。\n包含多行内容。";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        SourceContent content = parser.parse(bytes, "test.txt");

        assertNotNull(content.getSourceId());
        assertEquals("test.txt", content.getTitle());
        assertEquals(SourceContent.SourceType.DOCUMENT, content.getType());
        assertFalse(content.getRawText().isEmpty());
        assertTrue(content.getContentHash().startsWith("sha256:"));
    }

    @Test
    void shouldParseMarkdownContent() throws Exception {
        String markdown = "# 标题\n\n## 子标题\n\n这是正文内容。\n\n- 列表项1\n- 列表项2";
        byte[] bytes = markdown.getBytes(StandardCharsets.UTF_8);

        SourceContent content = parser.parse(bytes, "readme.md");

        assertNotNull(content.getRawText());
        assertTrue(content.getRawText().contains("标题"));
    }

    @Test
    void shouldGenerateConsistentHash() throws Exception {
        String text = "相同的内容应该产生相同的哈希值";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        SourceContent content1 = parser.parse(bytes, "a.txt");
        SourceContent content2 = parser.parse(bytes, "b.txt");

        assertEquals(content1.getContentHash(), content2.getContentHash());
    }
}
