package com.example.precip.template;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.model.TemplateDef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateLoaderTest {

    @TempDir
    Path tempDir;

    private KnowledgeBaseConfig config;
    private TemplateLoader loader;

    @BeforeEach
    void setUp() throws IOException {
        config = new KnowledgeBaseConfig();
        config.setBaseDir(tempDir);
        Files.createDirectories(config.templatesDir());
        Files.createDirectories(config.pagesDir());
        loader = new TemplateLoader(config);
    }

    @Test
    void shouldLoadTemplateWithCorrectCategoryName() throws IOException {
        String content = "# 产品文档\n\n一些内容\n\n# 技术文档\n\n更多内容";
        Files.writeString(config.templatesDir().resolve("knowledgeBaseOutput.md"), content);

        Map<String, TemplateDef> templates = loader.loadTemplates();

        assertEquals(1, templates.size());
        assertTrue(templates.containsKey("knowledge-base-output"));

        TemplateDef def = templates.get("knowledge-base-output");
        assertEquals("knowledgeBaseOutput.md", def.fileName());
        assertEquals(content, def.content());
    }

    @Test
    void shouldExtractH1Sections() throws IOException {
        String content = "# 产品对外文档\n\n段落1\n\n# 方案支撑文档\n\n段落2\n\n# 产品对内文档\n\n段落3";
        Files.writeString(config.templatesDir().resolve("test.md"), content);

        Map<String, TemplateDef> templates = loader.loadTemplates();
        TemplateDef def = templates.get("test");

        assertEquals(3, def.sections().size());
        assertEquals("产品对外文档", def.sections().get(0));
        assertEquals("方案支撑文档", def.sections().get(1));
        assertEquals("产品对内文档", def.sections().get(2));
    }

    @Test
    void shouldReturnEmptyMapWhenNoTemplates() {
        Map<String, TemplateDef> templates = loader.loadTemplates();
        assertTrue(templates.isEmpty());
    }

    @Test
    void shouldSplitBySections() throws IOException {
        String content = "# 段落一\n\n内容1\n\n# 段落二\n\n内容2";
        Files.writeString(config.templatesDir().resolve("split.md"), content);

        Map<String, TemplateDef> templates = loader.loadTemplates();
        TemplateDef def = templates.get("split");

        var parts = def.splitBySections();
        assertEquals(2, parts.size());
        assertTrue(parts.get(0).startsWith("# 段落一"));
        assertTrue(parts.get(1).startsWith("# 段落二"));
    }

    @Test
    void shouldConvertCamelCaseToKebabCase() {
        assertEquals("knowledge-base-output", TemplateDef.toCategoryName("knowledgeBaseOutput.md"));
        assertEquals("entity", TemplateDef.toCategoryName("entity.md"));
        assertEquals("api-reference", TemplateDef.toCategoryName("apiReference.md"));
    }

    @Test
    void shouldReloadTemplates() throws IOException {
        Files.writeString(config.templatesDir().resolve("a.md"), "# A");
        assertEquals(1, loader.loadTemplates().size());

        Files.writeString(config.templatesDir().resolve("b.md"), "# B");
        assertEquals(1, loader.loadTemplates().size()); // 缓存未变

        assertEquals(2, loader.reload().size()); // 强制重载
    }
}
