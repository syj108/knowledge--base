package com.example.precip.kb;

import com.example.precip.llm.LlmClient;
import com.example.precip.llm.PromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * 索引重建：扫描 pages/ 目录，生成 index.md（LLM 简介 + 程序化目录列表）。
 */
@Service
public class IndexGenerator {

    private static final Logger log = LoggerFactory.getLogger(IndexGenerator.class);

    private final KnowledgeBaseService kbService;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;

    public IndexGenerator(KnowledgeBaseService kbService, LlmClient llmClient,
                          PromptBuilder promptBuilder) {
        this.kbService = kbService;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
    }

    public void regenerate() throws IOException {
        List<String> allSlugs = kbService.allPageSlugs();
        if (allSlugs.isEmpty()) {
            kbService.writeIndex("# 知识库\n\n暂无内容。\n");
            return;
        }

        // 按分类分组
        Map<String, List<String>> categorized = new LinkedHashMap<>();
        for (String slug : allSlugs) {
            int slash = slug.indexOf('/');
            String category = slash > 0 ? slug.substring(0, slash) : "uncategorized";
            categorized.computeIfAbsent(category, k -> new ArrayList<>()).add(slug);
        }

        // LLM 生成简介
        String intro;
        try {
            String introPrompt = promptBuilder.buildIndexIntroPrompt(categorized);
            intro = llmClient.chat("你是知识库管理员。", introPrompt);
        } catch (Exception e) {
            log.warn("LLM 生成索引简介失败，使用默认简介: {}", e.getMessage());
            intro = "知识库包含 " + allSlugs.size() + " 篇文档，涵盖 " + categorized.size() + " 个分类。";
        }

        // 构建 index.md
        StringBuilder sb = new StringBuilder();
        sb.append("# 知识库索引\n\n");
        sb.append(intro.trim()).append("\n\n");
        sb.append("---\n\n");

        for (var entry : categorized.entrySet()) {
            String category = entry.getKey();
            List<String> slugs = entry.getValue();
            sb.append("## ").append(category)
                    .append(" (").append(slugs.size()).append(" 篇)\n\n");

            for (String slug : slugs) {
                String title = extractPageTitle(slug);
                sb.append("- [[").append(slug).append("|").append(title).append("]]\n");
            }
            sb.append("\n");
        }

        kbService.writeIndex(sb.toString());
        log.info("索引已重建，共 {} 篇文档、{} 个分类", allSlugs.size(), categorized.size());
    }

    private String extractPageTitle(String slug) {
        try {
            String content = kbService.readPage(slug);
            for (String line : content.split("\n")) {
                if (line.startsWith("# ")) {
                    return line.substring(2).trim();
                }
            }
        } catch (IOException e) {
            // 忽略
        }
        // 回退：从 slug 中提取标题
        int slash = slug.lastIndexOf('/');
        String name = slash >= 0 ? slug.substring(slash + 1) : slug;
        return name.replace('-', ' ');
    }
}
