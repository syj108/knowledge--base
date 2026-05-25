package com.example.precip.kb;

import com.example.precip.model.CategoryDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分类管理 REST 控制器：编辑、删除、页面迁移。
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Logger log = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryService categoryService;
    private final CategoryManagementService managementService;
    private final KnowledgeBaseService kbService;

    public CategoryController(CategoryService categoryService,
                               CategoryManagementService managementService,
                               KnowledgeBaseService kbService) {
        this.categoryService = categoryService;
        this.managementService = managementService;
        this.kbService = kbService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDef>> listCategories() {
        return ResponseEntity.ok(categoryService.listCategories());
    }

    @GetMapping("/{key}/pages")
    public ResponseEntity<List<String>> listCategoryPages(@PathVariable String key) {
        try {
            return ResponseEntity.ok(kbService.listPageSlugsInCategory(key));
        } catch (Exception e) {
            log.error("列出分类页面失败: {}", key, e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    @PutMapping("/{key}")
    public ResponseEntity<?> updateCategory(@PathVariable String key,
                                             @RequestBody EditCategoryRequest request) {
        try {
            CategoryDef updated = managementService.editCategory(
                    key, request.newKey(), request.name(), request.description());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("编辑分类失败: {}", key, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "编辑分类失败"));
        }
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<?> deleteCategory(@PathVariable String key,
                                             @RequestBody DeleteCategoryRequest request) {
        try {
            managementService.deleteCategory(key, request.action(),
                    request.targetCategoryKey(),
                    request.newCategoryKey(), request.newCategoryName(),
                    request.newCategoryDescription());
            return ResponseEntity.ok(Map.of("message", "分类已删除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("删除分类失败: {}", key, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "删除分类失败"));
        }
    }

    @PostMapping("/move-pages")
    public ResponseEntity<?> movePages(@RequestBody MovePagesRequest request) {
        try {
            if (request.pageSlugs() == null || request.pageSlugs().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "页面列表不能为空"));
            }
            if (request.targetCategoryKey() == null || request.targetCategoryKey().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "目标分类不能为空"));
            }
            managementService.movePages(request.pageSlugs(), request.targetCategoryKey());
            return ResponseEntity.ok(Map.of("message",
                    "已移动 " + request.pageSlugs().size() + " 个页面到分类 " + request.targetCategoryKey()));
        } catch (Exception e) {
            log.error("移动页面失败", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "移动页面失败"));
        }
    }

    record EditCategoryRequest(String newKey, String name, String description) {}

    record DeleteCategoryRequest(String action, String targetCategoryKey,
                                  String newCategoryKey, String newCategoryName,
                                  String newCategoryDescription) {}

    record MovePagesRequest(List<String> pageSlugs, String targetCategoryKey) {}
}
