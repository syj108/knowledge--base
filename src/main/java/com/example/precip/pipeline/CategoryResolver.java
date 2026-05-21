package com.example.precip.pipeline;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.model.KnowledgePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 分类目录验证与创建。
 * 确保 pages/{category}/ 目录存在，且 slug 前缀与 category 一致。
 */
@Service
public class CategoryResolver {

    private static final Logger log = LoggerFactory.getLogger(CategoryResolver.class);

    private final KnowledgeBaseConfig config;

    public CategoryResolver(KnowledgeBaseConfig config) {
        this.config = config;
    }

    public void resolve(List<KnowledgePage> pages) {
        for (KnowledgePage page : pages) {
            String category = page.getCategory();
            if (category == null || category.isBlank()) {
                log.warn("页面 {} 未设置分类，跳过目录创建", page.getSlug());
                continue;
            }

            // 确保 pages/{category}/ 目录存在
            Path categoryDir = config.pagesDir().resolve(category);
            try {
                Files.createDirectories(categoryDir);
            } catch (IOException e) {
                log.error("创建分类目录失败: {}", categoryDir, e);
            }

            // 校验 slug 前缀与 category 一致
            if (page.getSlug() != null && !page.getSlug().startsWith(category + "/")) {
                String correctedSlug = category + "/" + extractSlugName(page.getSlug());
                log.info("修正 slug 前缀: {} -> {}", page.getSlug(), correctedSlug);
                page.setSlug(correctedSlug);
            }
        }
    }

    private String extractSlugName(String slug) {
        int lastSlash = slug.lastIndexOf('/');
        return lastSlash >= 0 ? slug.substring(lastSlash + 1) : slug;
    }
}
