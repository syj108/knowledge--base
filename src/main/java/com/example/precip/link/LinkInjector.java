package com.example.precip.link;

import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.model.KnowledgePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 交叉链接注入：在页面正文中自动添加 [[slug|title]] 链接。
 * 仅在首次出现处注入，跳过代码块、已有链接、标题行和自引用。
 */
@Service
public class LinkInjector {

    private static final Logger log = LoggerFactory.getLogger(LinkInjector.class);

    private final KnowledgeBaseService kbService;
    private final WikiLinkParser wikiLinkParser;

    public LinkInjector(KnowledgeBaseService kbService, WikiLinkParser wikiLinkParser) {
        this.kbService = kbService;
        this.wikiLinkParser = wikiLinkParser;
    }

    public void inject(List<KnowledgePage> pages) {
        // 构建标题 → slug 映射（按标题长度降序排列，优先匹配更长的标题）
        List<TitleMapping> mappings = new ArrayList<>();
        for (KnowledgePage page : pages) {
            if (page.getTitle() != null && page.getSlug() != null) {
                mappings.add(new TitleMapping(page.getTitle(), page.getSlug()));
            }
        }
        // 加载已有页面
        try {
            for (String slug : kbService.allPageSlugs()) {
                String content = kbService.readPage(slug);
                String title = extractTitle(content);
                if (title != null) {
                    mappings.add(new TitleMapping(title, slug));
                }
            }
        } catch (IOException e) {
            log.warn("加载已有页面时出错，仅在新页面间注入: {}", e.getMessage());
        }

        // 按标题长度降序排序
        mappings.sort((a, b) -> Integer.compare(b.title.length(), a.title.length()));

        // 对每个新页面注入链接
        for (KnowledgePage page : pages) {
            if (page.getContent() == null) continue;
            page.setContent(injectLinks(page.getContent(), page.getSlug(), mappings));
        }
    }

    String injectLinks(String content, String selfSlug, List<TitleMapping> mappings) {
        String[] lines = content.split("\n", -1);
        boolean inCodeBlock = false;
        Set<String> injectedSlugs = new HashSet<>();

        // 收集已有的链接 slug，避免重复注入
        for (WikiLinkParser.WikiLink link : wikiLinkParser.parse(content)) {
            injectedSlugs.add(link.slug());
        }

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 追踪代码块状态
            if (line.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                continue;
            }
            if (inCodeBlock) continue;

            // 跳过标题行
            if (line.startsWith("#")) continue;

            // 尝试注入链接
            for (TitleMapping m : mappings) {
                // 跳过自引用
                if (m.slug.equals(selfSlug)) continue;
                // 已注入过此 slug
                if (injectedSlugs.contains(m.slug)) continue;

                int idx = line.indexOf(m.title);
                if (idx >= 0) {
                    // 检查是否已经在 [[ ]] 内
                    String before = line.substring(0, idx);
                    if (before.contains("[[") && !before.contains("]]")) continue;

                    String replacement = "[[" + m.slug + "|" + m.title + "]]";
                    lines[i] = line.substring(0, idx) + replacement
                            + line.substring(idx + m.title.length());
                    injectedSlugs.add(m.slug);
                    // 更新 line 以防同一行有多处匹配
                    line = lines[i];
                }
            }
        }

        return String.join("\n", lines);
    }

    private String extractTitle(String content) {
        // 从 Markdown 内容中提取第一个 H1 标题
        for (String line : content.split("\n")) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
            }
        }
        return null;
    }

    record TitleMapping(String title, String slug) {}
}
