package com.example.precip.kb;

import com.example.precip.model.GraphData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

/**
 * 知识页面 REST 控制器：页面列表、详情、搜索、图谱。
 */
@RestController
@RequestMapping("/api")
public class PageController {

    private static final Logger log = LoggerFactory.getLogger(PageController.class);

    private final KnowledgeBaseService kbService;
    private final SearchService searchService;

    public PageController(KnowledgeBaseService kbService, SearchService searchService) {
        this.kbService = kbService;
        this.searchService = searchService;
    }

    @GetMapping("/pages")
    public ResponseEntity<List<PageSummary>> listPages() {
        try {
            List<String> slugs = kbService.allPageSlugs();
            List<PageSummary> pages = new ArrayList<>();
            for (String slug : slugs) {
                String content = kbService.readPage(slug);
                String title = extractTitle(content, slug);
                String category = extractCategory(slug);
                String summary = extractSummary(content);
                pages.add(new PageSummary(slug, title, category, summary));
            }
            return ResponseEntity.ok(pages);
        } catch (IOException e) {
            log.error("列出页面失败", e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    @GetMapping("/pages/search")
    public ResponseEntity<List<SearchService.SearchResult>> search(@RequestParam("q") String query) {
        try {
            return ResponseEntity.ok(searchService.search(query));
        } catch (IOException e) {
            log.error("搜索失败", e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    @GetMapping("/pages/{*slug}")
    public ResponseEntity<PageDetail> getPage(@PathVariable String slug) {
        try {
            // Spring 的 {*slug} 会在前面加 /，需要去除
            if (slug.startsWith("/")) slug = slug.substring(1);

            if (!kbService.pageExists(slug)) {
                return ResponseEntity.notFound().build();
            }
            String content = kbService.readPage(slug);
            String title = extractTitle(content, slug);
            String category = extractCategory(slug);
            return ResponseEntity.ok(new PageDetail(slug, title, category, content));
        } catch (IOException e) {
            log.error("获取页面失败: {}", slug, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PatchMapping("/pages/{*slug}")
    public ResponseEntity<PageDetail> updatePageTitle(@PathVariable String slug,
                                                      @RequestBody Map<String, String> body) {
        try {
            if (slug.startsWith("/")) slug = slug.substring(1);
            if (!kbService.pageExists(slug)) {
                return ResponseEntity.notFound().build();
            }

            String newTitle = body.get("title");
            if (newTitle == null || newTitle.isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            String content = kbService.readPage(slug);
            // 替换或插入一级标题
            String[] lines = content.split("\n", -1);
            boolean replaced = false;
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (!replaced && line.startsWith("# ")) {
                    sb.append("# ").append(newTitle.trim()).append("\n");
                    replaced = true;
                } else {
                    sb.append(line).append("\n");
                }
            }
            if (!replaced) {
                sb.insert(0, "# " + newTitle.trim() + "\n\n");
            }
            // 去掉末尾多余换行
            String updatedContent = sb.toString().replaceAll("\n+$", "\n");
            kbService.writePage(slug, updatedContent);

            String category = extractCategory(slug);
            return ResponseEntity.ok(new PageDetail(slug, newTitle.trim(), category, updatedContent));
        } catch (IOException e) {
            log.error("更新页面标题失败: {}", slug, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/graph")
    public ResponseEntity<GraphData> getGraph() {
        try {
            return ResponseEntity.ok(kbService.readGraphData());
        } catch (IOException e) {
            log.error("获取图谱失败", e);
            return ResponseEntity.internalServerError().build();
        }
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

    private String extractSummary(String content) {
        // 跳过标题行，取正文前 150 字符
        StringBuilder sb = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.startsWith("#")) continue;
            if (line.isBlank()) continue;
            sb.append(line).append(" ");
            if (sb.length() > 150) break;
        }
        String summary = sb.toString().trim();
        return summary.length() > 150 ? summary.substring(0, 150) + "..." : summary;
    }

    public record PageSummary(String slug, String title, String category, String summary) {}
    public record PageDetail(String slug, String title, String category, String content) {}
}
