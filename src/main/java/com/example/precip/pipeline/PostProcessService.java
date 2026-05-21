package com.example.precip.pipeline;

import com.example.precip.kb.IndexGenerator;
import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.kb.StateService;
import com.example.precip.link.DeadLinkCleaner;
import com.example.precip.link.LinkGraph;
import com.example.precip.link.LinkInjector;
import com.example.precip.model.KnowledgePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 后处理编排：交叉链接注入 → 死链清理 → 页面写入 → 图谱更新 → 索引重建 → 状态标记完成。
 */
@Service
public class PostProcessService {

    private static final Logger log = LoggerFactory.getLogger(PostProcessService.class);

    private final LinkInjector linkInjector;
    private final DeadLinkCleaner deadLinkCleaner;
    private final KnowledgeBaseService kbService;
    private final LinkGraph linkGraph;
    private final IndexGenerator indexGenerator;
    private final StateService stateService;

    public PostProcessService(LinkInjector linkInjector, DeadLinkCleaner deadLinkCleaner,
                              KnowledgeBaseService kbService, LinkGraph linkGraph,
                              IndexGenerator indexGenerator, StateService stateService) {
        this.linkInjector = linkInjector;
        this.deadLinkCleaner = deadLinkCleaner;
        this.kbService = kbService;
        this.linkGraph = linkGraph;
        this.indexGenerator = indexGenerator;
        this.stateService = stateService;
    }

    public void process(List<KnowledgePage> pages, String sourceId) throws Exception {
        log.info("开始后处理，共 {} 个页面", pages.size());

        // 1. 交叉链接注入
        linkInjector.inject(pages);

        // 2. 死链清理
        deadLinkCleaner.clean(pages);

        // 3. 原子写入每个页面（预加标题）
        for (KnowledgePage page : pages) {
            if (page.getContent() != null && page.getSlug() != null) {
                String content = ensureTitleHeading(page.getTitle(), page.getContent());
                kbService.writePage(page.getSlug(), content);
                log.debug("已写入页面: {}", page.getSlug());
            }
        }

        // 4. 更新图谱
        linkGraph.updateGraph(pages);

        // 5. 重建索引
        indexGenerator.regenerate();

        // 6. 标记源处理完成
        List<String> generatedSlugs = pages.stream()
                .map(KnowledgePage::getSlug)
                .collect(Collectors.toList());
        stateService.markCompleted(sourceId, generatedSlugs);

        log.info("后处理完成，共生成 {} 个页面", generatedSlugs.size());
    }

    private String ensureTitleHeading(String title, String content) {
        if (title == null || title.isBlank()) return content;
        // 如果内容已经以正确的标题开头，直接返回
        String expectedHeading = "# " + title.trim();
        if (content.startsWith(expectedHeading)) return content;
        // 去除 LLM 生成的第一行（如果是模板章节标题如 "# 产品对外文档"）
        String trimmed = content;
        if (trimmed.startsWith("# ")) {
            int newline = trimmed.indexOf('\n');
            if (newline > 0) {
                trimmed = trimmed.substring(newline + 1).stripLeading();
            }
        }
        return expectedHeading + "\n\n" + trimmed;
    }
}
