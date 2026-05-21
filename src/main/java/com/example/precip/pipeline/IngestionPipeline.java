package com.example.precip.pipeline;

import com.example.precip.ingest.SourceIngestedEvent;
import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.kb.StateService;
import com.example.precip.model.*;
import com.example.precip.template.TemplateLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 主编排器：接收 SourceIngestedEvent，按顺序调用 Map → Reduce → Post 各阶段。
 */
@Service
public class IngestionPipeline {

    private static final Logger log = LoggerFactory.getLogger(IngestionPipeline.class);
    private static final int SPLIT_THRESHOLD = 15000;

    private final ExtractionService extractionService;
    private final GenerationService generationService;
    private final DeduplicationService deduplicationService;
    private final CategoryResolver categoryResolver;
    private final PostProcessService postProcessService;
    private final TemplateLoader templateLoader;
    private final KnowledgeBaseService kbService;
    private final StateService stateService;

    public IngestionPipeline(ExtractionService extractionService,
                             GenerationService generationService,
                             DeduplicationService deduplicationService,
                             CategoryResolver categoryResolver,
                             PostProcessService postProcessService,
                             TemplateLoader templateLoader,
                             KnowledgeBaseService kbService,
                             StateService stateService) {
        this.extractionService = extractionService;
        this.generationService = generationService;
        this.deduplicationService = deduplicationService;
        this.categoryResolver = categoryResolver;
        this.postProcessService = postProcessService;
        this.templateLoader = templateLoader;
        this.kbService = kbService;
        this.stateService = stateService;
    }

    @EventListener
    @Async
    public void onSourceIngested(SourceIngestedEvent event) {
        execute(event.getSourceId());
    }

    public void execute(String sourceId) {
        try {
            log.info("开始处理源: {}", sourceId);

            // 读取源内容（从归档的源文件中重新加载）
            SourceContent source = loadSourceContent(sourceId);
            if (source == null) {
                log.error("无法加载源内容: {}", sourceId);
                stateService.markFailed(sourceId);
                return;
            }

            Map<String, TemplateDef> templates = templateLoader.loadTemplates();

            // 读取用户指定的分类
            String assignedCategory = stateService.getSourceRecord(sourceId).getAssignedCategory();

            // --- Pass 0: 候选提取 ---
            stateService.updateSourceStatus(sourceId, "extracting");
            List<PageCandidate> candidates = extractionService.extractCandidates(
                    source, templates, assignedCategory);
            if (candidates.isEmpty()) {
                log.warn("未从源 {} 中提取到候选", sourceId);
                stateService.markFailed(sourceId);
                return;
            }

            // --- Pass 1: 模板填充 ---
            stateService.updateSourceStatus(sourceId, "generating");
            List<KnowledgePage> pages = generationService.generatePages(candidates, templates, source);

            // 检查是否建议拆分
            for (KnowledgePage page : pages) {
                if (page.getContent() != null && page.getContent().length() > SPLIT_THRESHOLD) {
                    stateService.markSplitSuggested(sourceId);
                    log.info("页面 {} 内容超过阈值（{}字符），建议拆分",
                            page.getSlug(), page.getContent().length());
                }
            }

            // --- Reduce: 去重 + 分类 ---
            stateService.updateSourceStatus(sourceId, "reducing");
            List<KnowledgePage> resolvedPages = deduplicationService.deduplicate(pages);
            categoryResolver.resolve(resolvedPages);

            // --- Post-processing ---
            postProcessService.process(resolvedPages, sourceId);

            log.info("源 {} 处理完成", sourceId);
        } catch (Exception e) {
            log.error("处理源 {} 失败", sourceId, e);
            try {
                stateService.markFailed(sourceId);
            } catch (Exception ex) {
                log.error("标记失败状态也失败了", ex);
            }
        }
    }

    private SourceContent loadSourceContent(String sourceId) {
        try {
            AgentState.SourceRecord record = stateService.getSourceRecord(sourceId);
            if (record == null) return null;

            Path sourceDir = kbService.getSourceDir(sourceId);
            if (Files.notExists(sourceDir)) return null;

            // 优先读取提取的纯文本文件（由摄取阶段保存）
            Path contentFile = sourceDir.resolve("_content.txt");
            String rawText;
            if (Files.exists(contentFile)) {
                rawText = Files.readString(contentFile, StandardCharsets.UTF_8);
            } else {
                // 回退：尝试读取目录下的第一个非元数据文件
                try (var files = Files.list(sourceDir)) {
                    Path firstFile = files
                            .filter(p -> !p.getFileName().toString().startsWith("_"))
                            .findFirst().orElse(null);
                    if (firstFile == null) return null;
                    rawText = Files.readString(firstFile, StandardCharsets.UTF_8);
                }
            }

            SourceContent content = new SourceContent();
            content.setSourceId(sourceId);
            content.setTitle(record.getTitle());
            content.setType(SourceContent.SourceType.valueOf(record.getType()));
            content.setRawText(rawText);
            content.setContentHash(record.getContentHash());
            return content;
        } catch (Exception e) {
            log.error("加载源内容失败: {}", sourceId, e);
            return null;
        }
    }
}
