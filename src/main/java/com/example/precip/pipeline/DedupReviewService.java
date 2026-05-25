package com.example.precip.pipeline;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.kb.IndexGenerator;
import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.kb.StateService;
import com.example.precip.link.LinkGraph;
import com.example.precip.llm.LlmClient;
import com.example.precip.llm.PromptBuilder;
import com.example.precip.model.DedupReview;
import com.example.precip.model.KnowledgePage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 去重审核服务：管理待审记录的生命周期，执行合并或独立保存。
 */
@Service
public class DedupReviewService {

    private static final Logger log = LoggerFactory.getLogger(DedupReviewService.class);

    private final KnowledgeBaseConfig config;
    private final KnowledgeBaseService kbService;
    private final StateService stateService;
    private final LinkGraph linkGraph;
    private final IndexGenerator indexGenerator;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final AtomicReference<List<DedupReview>> cache = new AtomicReference<>();
    private final DedupReviewService self;

    public DedupReviewService(KnowledgeBaseConfig config,
                               KnowledgeBaseService kbService,
                               StateService stateService,
                               LinkGraph linkGraph,
                               IndexGenerator indexGenerator,
                               LlmClient llmClient,
                               PromptBuilder promptBuilder,
                               @Lazy DedupReviewService self) {
        this.config = config;
        this.kbService = kbService;
        this.stateService = stateService;
        this.linkGraph = linkGraph;
        this.indexGenerator = indexGenerator;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.self = self;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public synchronized void addReview(DedupReview review) throws IOException {
        List<DedupReview> reviews = new ArrayList<>(loadReviews());
        reviews.add(review);
        saveReviews(reviews);
        log.info("添加去重审核记录: {} (新页面: {} vs 已有: {})",
                review.getId(), review.getNewPageTitle(), review.getExistingPageTitle());
    }

    public List<DedupReview> listPending() {
        return loadReviews().stream()
                .filter(r -> "pending".equals(r.getStatus()) || "resolving".equals(r.getStatus()))
                .toList();
    }

    public DedupReview getReview(String id) {
        return loadReviews().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 同步部分：校验参数、标记状态为 resolving，立即返回。
     * 实际处理由异步方法完成。
     */
    public synchronized void resolve(String id, String action) throws IOException {
        List<DedupReview> reviews = new ArrayList<>(loadReviews());
        DedupReview target = null;
        for (DedupReview r : reviews) {
            if (r.getId().equals(id)) {
                target = r;
                break;
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("审核记录不存在: " + id);
        }
        if (!"pending".equals(target.getStatus())) {
            throw new IllegalStateException("审核记录已处理: " + id);
        }

        // 立即标记为处理中，前端可据此判断状态
        target.setStatus("resolving");
        saveReviews(reviews);

        // 通过代理调用以确保 @Async 生效
        self.executeResolveAsync(id, action);
    }

    @Async
    public void executeResolveAsync(String id, String action) {
        try {
            DedupReview target = getReview(id);
            if (target == null) {
                log.error("异步执行审核时记录丢失: {}", id);
                return;
            }

            switch (action) {
                case "merge" -> executeMerge(target);
                case "separate" -> executeSeparate(target);
                default -> log.error("无效操作: {}", action);
            }

            markResolved(id, "merge".equals(action) ? "merged" : "separated");
            log.info("审核处理完成: {} -> {}", id, action);
        } catch (Exception e) {
            log.error("异步执行审核失败: {}", id, e);
            try {
                markResolved(id, "failed");
            } catch (IOException ex) {
                log.error("标记失败状态也失败了", ex);
            }
        }
    }

    private synchronized void markResolved(String id, String status) throws IOException {
        List<DedupReview> reviews = new ArrayList<>(loadReviews());
        for (DedupReview r : reviews) {
            if (r.getId().equals(id)) {
                r.setStatus(status);
                break;
            }
        }
        saveReviews(reviews);
    }

    private void executeMerge(DedupReview review) throws IOException {
        String existingContent = kbService.readPage(review.getExistingPageSlug());
        String newContent = review.getNewPageContent();

        // LLM 合并两份内容
        String mergePrompt = promptBuilder.buildMergePrompt(existingContent, newContent);
        String mergedContent = llmClient.chat("你是知识文档合并专家。", mergePrompt);

        // 确保标题行
        String title = review.getExistingPageTitle();
        if (!mergedContent.startsWith("# ")) {
            mergedContent = "# " + title + "\n\n" + mergedContent;
        }

        // 覆盖已有页面
        kbService.writePage(review.getExistingPageSlug(), mergedContent);

        // 更新图谱
        KnowledgePage mergedPage = new KnowledgePage();
        mergedPage.setSlug(review.getExistingPageSlug());
        mergedPage.setTitle(title);
        mergedPage.setCategory(review.getExistingPageCategory());
        mergedPage.setContent(mergedContent);
        linkGraph.updateGraph(List.of(mergedPage));

        // 重建索引
        indexGenerator.regenerate();

        log.info("合并完成: 新内容已合并到 {}", review.getExistingPageSlug());
    }

    private void executeSeparate(DedupReview review) throws IOException {
        String content = review.getNewPageContent();
        String title = review.getNewPageTitle();

        // 确保标题行
        if (content != null && !content.startsWith("# ")) {
            content = "# " + title + "\n\n" + content;
        }

        // 写入为新页面
        kbService.writePage(review.getNewPageSlug(), content);

        // 更新图谱
        KnowledgePage page = new KnowledgePage();
        page.setSlug(review.getNewPageSlug());
        page.setTitle(title);
        page.setCategory(review.getNewPageCategory());
        page.setContent(content);
        linkGraph.updateGraph(List.of(page));

        // 更新 state.json
        stateService.appendGeneratedPage(review.getSourceId(), review.getNewPageSlug());

        // 重建索引
        indexGenerator.regenerate();

        log.info("独立保存完成: {}", review.getNewPageSlug());
    }

    // --- 持久化 ---

    private List<DedupReview> loadReviews() {
        List<DedupReview> cached = cache.get();
        if (cached != null) return cached;

        Path file = config.dedupReviewsFile();
        if (Files.notExists(file)) {
            List<DedupReview> empty = new ArrayList<>();
            cache.set(empty);
            return empty;
        }
        try {
            List<DedupReview> reviews = objectMapper.readValue(
                    file.toFile(), new TypeReference<>() {});
            cache.set(reviews);
            return reviews;
        } catch (IOException e) {
            log.error("读取 dedup-reviews.json 失败", e);
            return new ArrayList<>();
        }
    }

    private void saveReviews(List<DedupReview> reviews) throws IOException {
        Path file = config.dedupReviewsFile();
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        String json = objectMapper.writeValueAsString(reviews);
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        cache.set(reviews);
    }
}
