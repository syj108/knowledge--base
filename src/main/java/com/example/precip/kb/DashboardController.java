package com.example.precip.kb;

import com.example.precip.model.AgentState;
import com.example.precip.model.GraphData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;

/**
 * 仪表盘统计 API。
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final KnowledgeBaseService kbService;
    private final StateService stateService;

    public DashboardController(KnowledgeBaseService kbService, StateService stateService) {
        this.kbService = kbService;
        this.stateService = stateService;
    }

    @GetMapping
    public ResponseEntity<DashboardStats> getDashboard() {
        try {
            AgentState state = stateService.getState();
            GraphData graph = kbService.readGraphData();
            List<String> allSlugs = kbService.allPageSlugs();

            int totalSources = state.getSources().size();
            int totalPages = allSlugs.size();
            int totalLinks = graph.getEdges().size();

            // 按分类统计页面数
            Map<String, Integer> categoryCounts = new LinkedHashMap<>();
            for (String slug : allSlugs) {
                int slash = slug.indexOf('/');
                String category = slash > 0 ? slug.substring(0, slash) : "uncategorized";
                categoryCounts.merge(category, 1, Integer::sum);
            }

            // 最近 5 个源文档（按时间倒序）
            List<RecentSource> recentSources = state.getSources().entrySet().stream()
                    .sorted((a, b) -> {
                        var ta = a.getValue().getLastIngestedAt();
                        var tb = b.getValue().getLastIngestedAt();
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    })
                    .limit(5)
                    .map(e -> new RecentSource(
                            e.getKey(),
                            e.getValue().getTitle(),
                            e.getValue().getType(),
                            e.getValue().getStatus(),
                            e.getValue().getLastIngestedAt() != null
                                    ? e.getValue().getLastIngestedAt().toString() : null))
                    .toList();

            return ResponseEntity.ok(new DashboardStats(
                    totalSources, totalPages, totalLinks,
                    categoryCounts.size(), categoryCounts, recentSources));
        } catch (IOException e) {
            log.error("获取仪表盘数据失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    public record DashboardStats(
            int totalSources, int totalPages, int totalLinks, int totalCategories,
            Map<String, Integer> categoryCounts, List<RecentSource> recentSources) {}

    public record RecentSource(
            String sourceId, String title, String type, String status, String lastIngestedAt) {}
}
