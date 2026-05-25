package com.example.precip.pipeline;

import com.example.precip.model.DedupReview;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 去重审核 REST 控制器。
 */
@RestController
@RequestMapping("/api/dedup-reviews")
public class DedupReviewController {

    private static final Logger log = LoggerFactory.getLogger(DedupReviewController.class);

    private final DedupReviewService reviewService;

    public DedupReviewController(DedupReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<List<DedupReview>> listPending() {
        return ResponseEntity.ok(reviewService.listPending());
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<?> resolve(@PathVariable String id,
                                      @RequestBody Map<String, String> body) {
        try {
            String action = body.get("action");
            if (action == null || (!action.equals("merge") && !action.equals("separate"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "action 必须为 merge 或 separate"));
            }
            reviewService.resolve(id, action);
            return ResponseEntity.ok(Map.of("message",
                    "merge".equals(action) ? "正在合并到已有页面，请稍候..." : "正在保存为独立页面，请稍候..."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("处理审核决策失败: {}", id, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "处理失败"));
        }
    }
}
