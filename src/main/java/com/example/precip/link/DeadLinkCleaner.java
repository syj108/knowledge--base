package com.example.precip.link;

import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.model.KnowledgePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 死链检测与清理：移除指向不存在页面的 [[]] 链接，保留显示名文本。
 */
@Service
public class DeadLinkCleaner {

    private static final Logger log = LoggerFactory.getLogger(DeadLinkCleaner.class);

    private final KnowledgeBaseService kbService;
    private final WikiLinkParser wikiLinkParser;

    public DeadLinkCleaner(KnowledgeBaseService kbService, WikiLinkParser wikiLinkParser) {
        this.kbService = kbService;
        this.wikiLinkParser = wikiLinkParser;
    }

    public void clean(List<KnowledgePage> pages) throws IOException {
        // 收集所有有效 slug：已有页面 + 新生成页面
        Set<String> validSlugs = new HashSet<>(kbService.allPageSlugs());
        for (KnowledgePage page : pages) {
            if (page.getSlug() != null) validSlugs.add(page.getSlug());
        }

        // 清理新页面中的死链
        for (KnowledgePage page : pages) {
            if (page.getContent() == null) continue;
            page.setContent(cleanDeadLinks(page.getContent(), validSlugs));
        }
    }

    String cleanDeadLinks(String content, Set<String> validSlugs) {
        List<WikiLinkParser.WikiLink> links = wikiLinkParser.parse(content);
        if (links.isEmpty()) return content;

        // 从后往前替换，避免偏移量变化
        StringBuilder sb = new StringBuilder(content);
        for (int i = links.size() - 1; i >= 0; i--) {
            WikiLinkParser.WikiLink link = links.get(i);
            if (!validSlugs.contains(link.slug())) {
                // 死链：去除 [[]] 语法，只保留显示名
                sb.replace(link.start(), link.end(), link.displayName());
                log.debug("移除死链: [[{}|{}]]", link.slug(), link.displayName());
            }
        }
        return sb.toString();
    }
}
