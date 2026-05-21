package com.example.precip.template;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.model.TemplateDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

/**
 * 扫描并加载知识库 templates/ 目录下的所有 .md 模板文件。
 */
@Service
public class TemplateLoader {

    private static final Logger log = LoggerFactory.getLogger(TemplateLoader.class);

    private final KnowledgeBaseConfig config;
    private final AtomicReference<Map<String, TemplateDef>> cache = new AtomicReference<>();

    public TemplateLoader(KnowledgeBaseConfig config) {
        this.config = config;
    }

    /**
     * 加载所有模板。结果按 categoryName 做 key。
     * 首次调用后缓存，后续调用返回缓存值。
     */
    public Map<String, TemplateDef> loadTemplates() {
        Map<String, TemplateDef> cached = cache.get();
        if (cached != null) {
            return cached;
        }
        Map<String, TemplateDef> result = doLoad();
        cache.set(result);
        return result;
    }

    /**
     * 强制重新加载模板（用于模板目录内容变更后）。
     */
    public Map<String, TemplateDef> reload() {
        cache.set(null);
        return loadTemplates();
    }

    private Map<String, TemplateDef> doLoad() {
        Map<String, TemplateDef> templates = new LinkedHashMap<>();
        Path templatesDir = config.templatesDir();

        if (Files.notExists(templatesDir)) {
            log.warn("模板目录不存在: {}", templatesDir);
            return templates;
        }

        try (Stream<Path> files = Files.list(templatesDir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".md"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            String content = Files.readString(path, StandardCharsets.UTF_8);
                            String categoryName = TemplateDef.toCategoryName(fileName);
                            var sections = TemplateDef.extractH1Sections(content);

                            TemplateDef def = new TemplateDef(fileName, categoryName, content, sections);
                            templates.put(categoryName, def);
                            log.info("已加载模板: {} → 分类 [{}]，包含 {} 个 H1 段落",
                                    fileName, categoryName, sections.size());
                        } catch (IOException e) {
                            log.error("读取模板文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("扫描模板目录失败: {}", templatesDir, e);
        }

        return templates;
    }
}
