package com.example.precip.kb;

import com.example.precip.model.CategoryDef;
import com.example.precip.model.GraphData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * 分类编辑、删除、页面迁移的编排服务。
 */
@Service
public class CategoryManagementService {

    private static final Logger log = LoggerFactory.getLogger(CategoryManagementService.class);

    private final CategoryService categoryService;
    private final KnowledgeBaseService kbService;
    private final StateService stateService;
    private final IndexGenerator indexGenerator;

    public CategoryManagementService(CategoryService categoryService,
                                     KnowledgeBaseService kbService,
                                     StateService stateService,
                                     IndexGenerator indexGenerator) {
        this.categoryService = categoryService;
        this.kbService = kbService;
        this.stateService = stateService;
        this.indexGenerator = indexGenerator;
    }

    // --- 编辑分类 ---

    public CategoryDef editCategory(String oldKey, String newKey,
                                     String newName, String newDescription) throws IOException {
        CategoryDef updated = categoryService.updateCategory(oldKey, newKey, newName, newDescription);
        String effectiveNewKey = updated.key();

        if (!effectiveNewKey.equals(oldKey)) {
            List<String> slugs = kbService.listPageSlugsInCategory(oldKey);
            for (String slug : slugs) {
                String pageName = slug.substring(slug.indexOf('/') + 1);
                String newSlug = effectiveNewKey + "/" + pageName;
                kbService.movePage(slug, newSlug);
            }
            stateService.replaceSlugPrefix(oldKey, effectiveNewKey);
            updateGraphCategory(oldKey, effectiveNewKey);
            replaceWikiLinksGlobally(oldKey, effectiveNewKey);
            kbService.deleteCategoryDir(oldKey);
            indexGenerator.regenerate();
            log.info("分类 key 迁移完成: {} → {}，涉及 {} 个页面", oldKey, effectiveNewKey, slugs.size());
        }

        return updated;
    }

    // --- 删除分类 ---

    public void deleteCategory(String categoryKey, String action,
                                String targetCategoryKey,
                                String newCategoryKey, String newCategoryName,
                                String newCategoryDescription) throws IOException {
        CategoryDef cat = categoryService.findByKey(categoryKey);
        if (cat == null) {
            throw new IllegalArgumentException("分类不存在: " + categoryKey);
        }

        List<String> slugs = kbService.listPageSlugsInCategory(categoryKey);

        switch (action) {
            case "DELETE_PAGES" -> {
                for (String slug : slugs) {
                    kbService.deletePage(slug);
                }
                stateService.removePageSlugs(slugs);
                removeGraphNodes(slugs);
                log.info("删除分类 {} 下的 {} 个页面", categoryKey, slugs.size());
            }
            case "MOVE_TO_EXISTING" -> {
                if (targetCategoryKey == null || targetCategoryKey.isBlank()) {
                    throw new IllegalArgumentException("需要指定目标分类");
                }
                movePagesToCategory(slugs, targetCategoryKey);
            }
            case "MOVE_TO_NEW" -> {
                if (newCategoryKey == null || newCategoryKey.isBlank()
                        || newCategoryName == null || newCategoryName.isBlank()) {
                    throw new IllegalArgumentException("需要指定新分类的 key 和名称");
                }
                categoryService.addCategory(newCategoryKey, newCategoryName,
                        newCategoryDescription != null ? newCategoryDescription : "");
                movePagesToCategory(slugs, newCategoryKey);
            }
            case "MOVE_TO_UNCATEGORIZED" -> {
                ensureUncategorized();
                movePagesToCategory(slugs, "uncategorized");
            }
            default -> throw new IllegalArgumentException("未知操作: " + action);
        }

        categoryService.deleteCategory(categoryKey);
        kbService.deleteCategoryDir(categoryKey);
        indexGenerator.regenerate();
    }

    // --- 批量移动页面 ---

    public void movePages(List<String> pageSlugs, String targetCategoryKey) throws IOException {
        movePagesToCategory(pageSlugs, targetCategoryKey);
        indexGenerator.regenerate();
    }

    // --- 内部方法 ---

    private void movePagesToCategory(List<String> slugs, String targetCategoryKey) throws IOException {
        for (String slug : slugs) {
            String pageName = slug.contains("/") ? slug.substring(slug.indexOf('/') + 1) : slug;
            String newSlug = targetCategoryKey + "/" + pageName;
            if (slug.equals(newSlug)) continue;

            kbService.movePage(slug, newSlug);
            stateService.movePageSlug(slug, newSlug);
            updateGraphNode(slug, newSlug, targetCategoryKey);
            replaceWikiLinkInAllPages(slug, newSlug);
        }
        log.info("已移动 {} 个页面到分类 {}", slugs.size(), targetCategoryKey);
    }

    private void updateGraphCategory(String oldCategoryKey, String newCategoryKey) throws IOException {
        GraphData graph = kbService.readGraphData();
        Map<String, GraphData.GraphNode> nodes = graph.getNodes();
        Map<String, GraphData.GraphNode> updated = new LinkedHashMap<>();

        for (var entry : nodes.entrySet()) {
            String key = entry.getKey();
            GraphData.GraphNode node = entry.getValue();
            if (key.startsWith(oldCategoryKey + "/")) {
                String newKey = newCategoryKey + key.substring(oldCategoryKey.length());
                node.setCategory(newCategoryKey);
                updated.put(newKey, node);
            } else {
                updated.put(key, node);
            }
        }
        graph.setNodes(updated);

        for (GraphData.GraphEdge edge : graph.getEdges()) {
            if (edge.getSource().startsWith(oldCategoryKey + "/")) {
                edge.setSource(newCategoryKey + edge.getSource().substring(oldCategoryKey.length()));
            }
            if (edge.getTarget().startsWith(oldCategoryKey + "/")) {
                edge.setTarget(newCategoryKey + edge.getTarget().substring(oldCategoryKey.length()));
            }
        }

        kbService.writeGraphData(graph);
    }

    private void updateGraphNode(String oldSlug, String newSlug, String newCategory) throws IOException {
        GraphData graph = kbService.readGraphData();
        GraphData.GraphNode node = graph.getNodes().remove(oldSlug);
        if (node != null) {
            node.setCategory(newCategory);
            graph.getNodes().put(newSlug, node);
        }
        for (GraphData.GraphEdge edge : graph.getEdges()) {
            if (edge.getSource().equals(oldSlug)) edge.setSource(newSlug);
            if (edge.getTarget().equals(oldSlug)) edge.setTarget(newSlug);
        }
        kbService.writeGraphData(graph);
    }

    private void removeGraphNodes(List<String> slugs) throws IOException {
        GraphData graph = kbService.readGraphData();
        Set<String> slugSet = new HashSet<>(slugs);
        for (String slug : slugs) {
            graph.getNodes().remove(slug);
        }
        graph.getEdges().removeIf(e -> slugSet.contains(e.getSource()) || slugSet.contains(e.getTarget()));
        kbService.writeGraphData(graph);
    }

    private void replaceWikiLinksGlobally(String oldCategoryKey, String newCategoryKey) throws IOException {
        List<String> allSlugs = kbService.allPageSlugs();
        String oldPrefix = "[[" + oldCategoryKey + "/";
        String newPrefix = "[[" + newCategoryKey + "/";
        for (String slug : allSlugs) {
            String content = kbService.readPage(slug);
            if (content.contains(oldPrefix)) {
                String updated = content.replace(oldPrefix, newPrefix);
                kbService.writePage(slug, updated);
            }
        }
    }

    private void replaceWikiLinkInAllPages(String oldSlug, String newSlug) throws IOException {
        List<String> allSlugs = kbService.allPageSlugs();
        String oldLink = "[[" + oldSlug;
        String newLink = "[[" + newSlug;
        for (String slug : allSlugs) {
            String content = kbService.readPage(slug);
            if (content.contains(oldLink)) {
                String updated = content.replace(oldLink, newLink);
                kbService.writePage(slug, updated);
            }
        }
    }

    private void ensureUncategorized() throws IOException {
        if (categoryService.findByKey("uncategorized") == null) {
            categoryService.addCategory("uncategorized", "未分类", "未归类的知识页面");
        }
    }
}
