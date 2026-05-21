package com.example.precip.pipeline;

import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.link.LinkGraph;
import com.example.precip.llm.LlmClient;
import com.example.precip.llm.PromptBuilder;
import com.example.precip.model.KnowledgePage;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 去重服务：检查新生成页面是否与已有页面重复。
 */
@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);
    private static final int SUMMARY_LENGTH = 500;

    private final KnowledgeBaseService kbService;
    private final LinkGraph linkGraph;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;

    public DeduplicationService(KnowledgeBaseService kbService, LinkGraph linkGraph,
                                LlmClient llmClient, PromptBuilder promptBuilder) {
        this.kbService = kbService;
        this.linkGraph = linkGraph;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
    }

    public List<KnowledgePage> deduplicate(List<KnowledgePage> pages) {
        List<KnowledgePage> result = new ArrayList<>();

        for (KnowledgePage page : pages) {
            try {
                if (!linkGraph.nodeExists(page.getSlug())) {
                    // 图谱中不存在，直接标记为新页面
                    page.setNew(true);
                    result.add(page);
                    log.debug("新页面: {}", page.getSlug());
                    continue;
                }

                // 已存在同 slug 节点——通过 LLM 判定
                String existingContent = kbService.readPage(page.getSlug());
                String existingSummary = truncateSummary(existingContent);
                String newSummary = truncateSummary(page.getContent());

                String deduplicationPrompt = promptBuilder.buildDeduplicationPrompt(existingSummary, newSummary);
                Map<String, String> decision = llmClient.chatForJson(
                        "你是一个去重判定助手。", deduplicationPrompt,
                        new TypeReference<>() {});

                String action = decision.getOrDefault("decision", "keep_both");
                switch (action) {
                    case "merge", "new_version" -> {
                        // 覆盖旧版本
                        page.setNew(false);
                        result.add(page);
                        log.info("页面 {} 将覆盖旧版本（决策: {}）", page.getSlug(), action);
                    }
                    case "keep_both" -> {
                        // 生成变体 slug
                        String variantSlug = generateVariantSlug(page.getSlug());
                        page.setSlug(variantSlug);
                        page.setNew(true);
                        result.add(page);
                        log.info("保留两者，新页面使用变体 slug: {}", variantSlug);
                    }
                    default -> {
                        page.setNew(false);
                        result.add(page);
                        log.warn("未知去重决策 '{}'，默认覆盖", action);
                    }
                }

            } catch (Exception e) {
                // 去重失败不阻塞流水线，默认当作新页面处理
                log.warn("去重检查失败（{}），按新页面处理: {}", page.getSlug(), e.getMessage());
                page.setNew(true);
                result.add(page);
            }
        }

        return result;
    }

    private String truncateSummary(String content) {
        if (content == null) return "";
        return content.length() <= SUMMARY_LENGTH
                ? content
                : content.substring(0, SUMMARY_LENGTH);
    }

    private String generateVariantSlug(String slug) {
        // 查找最小可用后缀
        for (int i = 2; i <= 99; i++) {
            String variant = slug + "-" + i;
            try {
                if (!kbService.pageExists(variant)) return variant;
            } catch (Exception e) {
                return slug + "-" + i;
            }
        }
        return slug + "-" + System.currentTimeMillis();
    }
}
