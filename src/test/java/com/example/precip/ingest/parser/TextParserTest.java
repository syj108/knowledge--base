package com.example.precip.ingest.parser;

import com.example.precip.ingest.SourcePreprocessor;
import com.example.precip.model.SourceContent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextParserTest {

    private final TextParser parser = new TextParser();
    private final SourcePreprocessor preprocessor = new SourcePreprocessor();

    @Test
    void shouldParseChineseText() throws Exception {
        Map<String, String> data = Map.of(
                "title", "测试文档",
                "content", "这是一段中文测试内容，用于验证文本解析器的功能。",
                "language", ""
        );
        SourceContent content = parser.parse(data, "测试文档");

        assertEquals("测试文档", content.getTitle());
        assertEquals(SourceContent.SourceType.FREE_TEXT, content.getType());
        assertEquals("zh", content.getLanguage());
        assertNotNull(content.getContentHash());
        assertTrue(content.getContentHash().startsWith("sha256:"));
    }

    @Test
    void shouldDetectChineseLanguage() {
        assertEquals("zh", preprocessor.detectLanguage("这是一段中文文本，测试语言检测功能"));
        assertEquals("en", preprocessor.detectLanguage("This is an English text for testing"));
    }

    @Test
    void shouldSplitByHeadings() {
        String text = "前言内容\n\n# 第一章\n\n内容1\n\n## 第一节\n\n内容1.1\n\n# 第二章\n\n内容2";
        List<String> sections = preprocessor.splitByHeadings(text);

        assertEquals(4, sections.size());
        assertEquals("前言内容", sections.get(0));
        assertTrue(sections.get(1).startsWith("# 第一章"));
        assertTrue(sections.get(2).startsWith("## 第一节"));
        assertTrue(sections.get(3).startsWith("# 第二章"));
    }

    @Test
    void shouldTruncateLongText() {
        String longText = "段落一\n\n".repeat(10000);
        String truncated = preprocessor.truncate(longText);

        assertTrue(truncated.length() < longText.length());
        assertTrue(truncated.endsWith("[... 内容已截断 ...]"));
    }

    @Test
    void shouldHandleExplicitLanguage() throws Exception {
        Map<String, String> data = Map.of(
                "title", "Test",
                "content", "Some English content",
                "language", "en"
        );
        SourceContent content = parser.parse(data, "Test");
        assertEquals("en", content.getLanguage());
    }
}
