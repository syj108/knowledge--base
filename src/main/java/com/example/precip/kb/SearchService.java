package com.example.precip.kb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 关键词搜索服务：遍历知识库页面进行全文匹配。
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);
    private static final int SNIPPET_RADIUS = 60;
    private static final int MAX_RESULTS = 50;

    private final KnowledgeBaseService kbService;

    public SearchService(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    public List<SearchResult> search(String query) throws IOException {
        if (query == null || query.isBlank()) return List.of();

        String lowerQuery = query.toLowerCase();
        List<SearchResult> results = new ArrayList<>();

        for (String slug : kbService.allPageSlugs()) {
            try {
                String content = kbService.readPage(slug);
                String title = extractTitle(content, slug);
                String category = extractCategory(slug);

                boolean titleMatch = title.toLowerCase().contains(lowerQuery);
                int contentIdx = content.toLowerCase().indexOf(lowerQuery);

                if (titleMatch || contentIdx >= 0) {
                    String snippet = "";
                    if (contentIdx >= 0) {
                        snippet = buildSnippet(content, contentIdx, query.length());
                    } else {
                        // 标题匹配但内容无匹配，取内容前 120 字符作为摘要
                        snippet = content.length() > 120
                                ? content.substring(0, 120) + "..."
                                : content;
                    }

                    int score = titleMatch ? 100 : 10;
                    if (titleMatch && contentIdx >= 0) score = 110;

                    results.add(new SearchResult(slug, title, category, snippet, score));
                }
            } catch (IOException e) {
                log.warn("读取页面 {} 失败，跳过: {}", slug, e.getMessage());
            }
        }

        results.sort(Comparator.comparingInt(SearchResult::score).reversed());
        if (results.size() > MAX_RESULTS) {
            results = results.subList(0, MAX_RESULTS);
        }

        return results;
    }

    private String buildSnippet(String content, int matchIdx, int queryLen) {
        int start = Math.max(0, matchIdx - SNIPPET_RADIUS);
        int end = Math.min(content.length(), matchIdx + queryLen + SNIPPET_RADIUS);

        String snippet = content.substring(start, end)
                .replace('\n', ' ')
                .replace('\r', ' ');

        if (start > 0) snippet = "..." + snippet;
        if (end < content.length()) snippet = snippet + "...";

        return snippet;
    }

    private String extractTitle(String content, String slug) {
        for (String line : content.split("\n")) {
            if (line.startsWith("# ")) {
                return line.substring(2).trim();
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

    public record SearchResult(String slug, String title, String category, String snippet, int score) {}
}
