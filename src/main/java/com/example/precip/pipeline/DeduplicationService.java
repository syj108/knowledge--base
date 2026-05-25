package com.example.precip.pipeline;

import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.llm.LlmClient;
import com.example.precip.llm.PromptBuilder;
import com.example.precip.model.DedupReview;
import com.example.precip.model.KnowledgePage;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * 去重服务：对新生成页面与知识库中已有页面做相似度匹配，
 * 结合标题相似度和 LLM 判断决定是否合并。
 */
@Service
public class DeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(DeduplicationService.class);
    private static final double SIMILARITY_THRESHOLD = 0.35;
    private static final int SUMMARY_LENGTH = 500;

    private final KnowledgeBaseService kbService;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;

    public DeduplicationService(KnowledgeBaseService kbService,
                                LlmClient llmClient, PromptBuilder promptBuilder) {
        this.kbService = kbService;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
    }

    /**
     * 去重结果：已确认的页面列表 + 需用户审核的记录列表。
     */
    public record DeduplicationResult(
            List<KnowledgePage> resolvedPages,
            List<DedupReview> pendingReviews
    ) {}

    public DeduplicationResult deduplicate(List<KnowledgePage> pages) {
        List<KnowledgePage> resolved = new ArrayList<>();
        List<DedupReview> pending = new ArrayList<>();

        for (KnowledgePage page : pages) {
            try {
                MatchResult match = findBestMatch(page);

                if (match == null || match.score < SIMILARITY_THRESHOLD) {
                    page.setNew(true);
                    resolved.add(page);
                    log.debug("新页面（无相似）: {}", page.getSlug());
                    continue;
                }

                log.info("页面 {} 与已有页面 {} 相似度 {:.2f}，调用 LLM 判断",
                        page.getSlug(), match.slug, match.score);

                // LLM 判断是否应合并
                String existingContent = kbService.readPage(match.slug);
                String existingSummary = truncate(existingContent, SUMMARY_LENGTH);
                String newSummary = truncate(page.getContent(), SUMMARY_LENGTH);

                String deduplicationPrompt = promptBuilder.buildDeduplicationPrompt(
                        match.title, existingSummary, page.getTitle(), newSummary);
                Map<String, String> decision = llmClient.chatForJson(
                        "你是一个知识去重判定助手。", deduplicationPrompt,
                        new TypeReference<>() {});

                String action = decision.getOrDefault("decision", "separate");
                String reason = decision.getOrDefault("reason", "");

                switch (action) {
                    case "separate" -> {
                        page.setNew(true);
                        resolved.add(page);
                        log.info("LLM 判断为独立页面: {} (reason: {})", page.getSlug(), reason);
                    }
                    case "merge", "uncertain" -> {
                        DedupReview review = buildReview(page, match.slug, match.title,
                                match.category, existingSummary, action, reason);
                        pending.add(review);
                        log.info("页面 {} 需用户审核（{}）: {}", page.getSlug(), action, reason);
                    }
                    default -> {
                        page.setNew(true);
                        resolved.add(page);
                        log.warn("未知去重决策 '{}'，按新页面处理", action);
                    }
                }

            } catch (Exception e) {
                log.warn("去重检查失败（{}），按新页面处理: {}", page.getSlug(), e.getMessage());
                page.setNew(true);
                resolved.add(page);
            }
        }

        log.info("去重完成：{} 个页面直接通过，{} 个待审核", resolved.size(), pending.size());
        return new DeduplicationResult(resolved, pending);
    }

    // --- 相似度匹配 ---

    private record MatchResult(String slug, String title, String category, double score) {}

    private MatchResult findBestMatch(KnowledgePage page) {
        try {
            // 优先检查同分类下的页面
            List<String> sameCategorySlugs = kbService.listPageSlugsInCategory(page.getCategory());
            // 也检查全库（避免跨分类的重复）
            List<String> allSlugs = kbService.allPageSlugs();

            Set<String> checked = new HashSet<>();
            MatchResult best = null;

            // 先查同分类
            for (String slug : sameCategorySlugs) {
                if (slug.equals(page.getSlug())) {
                    // 完全相同 slug，直接返回最高匹配
                    String content = kbService.readPage(slug);
                    String title = extractTitle(content, slug);
                    String category = extractCategory(slug);
                    return new MatchResult(slug, title, category, 1.0);
                }
                checked.add(slug);
                MatchResult result = computeMatch(page, slug);
                if (result != null && (best == null || result.score > best.score)) {
                    best = result;
                }
            }

            // 再查全库中同分类外的页面
            for (String slug : allSlugs) {
                if (checked.contains(slug) || slug.equals(page.getSlug())) continue;
                MatchResult result = computeMatch(page, slug);
                if (result != null && (best == null || result.score > best.score)) {
                    best = result;
                }
            }

            return best;
        } catch (Exception e) {
            log.warn("查找相似页面失败: {}", e.getMessage());
            return null;
        }
    }

    private MatchResult computeMatch(KnowledgePage page, String existingSlug) {
        try {
            String content = kbService.readPage(existingSlug);
            String existingTitle = extractTitle(content, existingSlug);
            double score = titleSimilarity(page.getTitle(), existingTitle);
            if (score >= SIMILARITY_THRESHOLD) {
                String category = extractCategory(existingSlug);
                return new MatchResult(existingSlug, existingTitle, category, score);
            }
        } catch (Exception e) {
            // 读取失败，跳过
        }
        return null;
    }

    // --- 标题相似度（Jaccard 系数）---

    static double titleSimilarity(String a, String b) {
        if (a == null || b == null) return 0;
        Set<String> tokensA = tokenize(a);
        Set<String> tokensB = tokenize(b);
        if (tokensA.isEmpty() && tokensB.isEmpty()) return 1.0;
        if (tokensA.isEmpty() || tokensB.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);

        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        return (double) intersection.size() / union.size();
    }

    private static Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        if (text == null) return tokens;
        // 按非字母数字字符分割，得到词语
        String normalized = text.toLowerCase().trim();
        String[] words = normalized.split("[\\s\\p{Punct}]+");
        for (String word : words) {
            if (word.isBlank()) continue;
            tokens.add(word);
            // 对中文：按单字拆分
            if (word.matches(".*[\\u4e00-\\u9fff].*")) {
                for (char c : word.toCharArray()) {
                    if (c >= '一' && c <= '鿿') {
                        tokens.add(String.valueOf(c));
                    }
                }
            }
        }
        return tokens;
    }

    // --- 辅助方法 ---

    private DedupReview buildReview(KnowledgePage page, String existingSlug,
                                     String existingTitle, String existingCategory,
                                     String existingSummary,
                                     String suggestion, String reason) {
        DedupReview review = new DedupReview();
        review.setId(UUID.randomUUID().toString());
        review.setSourceId(page.getSourceId());
        review.setNewPageTitle(page.getTitle());
        review.setNewPageCategory(page.getCategory());
        review.setNewPageSlug(page.getSlug());
        review.setNewPageSummary(truncate(page.getContent(), 300));
        review.setNewPageContent(page.getContent());
        review.setExistingPageSlug(existingSlug);
        review.setExistingPageTitle(existingTitle);
        review.setExistingPageCategory(existingCategory);
        review.setExistingPageSummary(existingSummary);
        review.setSuggestion(suggestion);
        review.setReason(reason);
        review.setStatus("pending");
        review.setCreatedAt(Instant.now());
        return review;
    }

    private String truncate(String content, int maxLength) {
        if (content == null) return "";
        return content.length() <= maxLength ? content : content.substring(0, maxLength);
    }

    private String extractTitle(String content, String slug) {
        if (content != null) {
            for (String line : content.split("\n")) {
                if (line.startsWith("# ")) {
                    return line.substring(2).trim();
                }
            }
        }
        int lastSlash = slug.lastIndexOf('/');
        String name = lastSlash >= 0 ? slug.substring(lastSlash + 1) : slug;
        return name.replace('-', ' ');
    }

    private String extractCategory(String slug) {
        int slash = slug.indexOf('/');
        return slash > 0 ? slug.substring(0, slash) : "uncategorized";
    }
}
