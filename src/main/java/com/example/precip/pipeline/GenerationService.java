package com.example.precip.pipeline;

import com.example.precip.ingest.SourcePreprocessor;
import com.example.precip.llm.LlmClient;
import com.example.precip.llm.PromptBuilder;
import com.example.precip.model.KnowledgePage;
import com.example.precip.model.PageCandidate;
import com.example.precip.model.SourceContent;
import com.example.precip.model.TemplateDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Pass 1：并行为每个候选填充模板，生成完整知识文档。
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);
    private static final int CONTEXT_LIMIT = 8000;

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final SourcePreprocessor preprocessor;
    private final ThreadPoolExecutor llmExecutor;

    public GenerationService(LlmClient llmClient, PromptBuilder promptBuilder,
                             SourcePreprocessor preprocessor,
                             @Qualifier("llmExecutor") ThreadPoolExecutor llmExecutor) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.preprocessor = preprocessor;
        this.llmExecutor = llmExecutor;
    }

    public List<KnowledgePage> generatePages(List<PageCandidate> candidates,
                                              Map<String, TemplateDef> templates,
                                              SourceContent source) {
        String sourceContext = preprocessor.truncate(source.getRawText());
        if (sourceContext.length() > CONTEXT_LIMIT) {
            sourceContext = sourceContext.substring(0, CONTEXT_LIMIT) + "\n\n[... 上下文已截断 ...]";
        }

        final String ctx = sourceContext;

        List<CompletableFuture<KnowledgePage>> futures = candidates.stream()
                .map(candidate -> {
                    TemplateDef template = templates.get(candidate.getCategory());
                    if (template == null) {
                        // 回退到第一个可用模板
                        template = templates.values().iterator().next();
                    }
                    final TemplateDef t = template;
                    return CompletableFuture.supplyAsync(
                            () -> generateSinglePage(candidate, t, ctx), llmExecutor);
                })
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private KnowledgePage generateSinglePage(PageCandidate candidate,
                                              TemplateDef template,
                                              String sourceContext) {
        log.info("生成页面: {} [{}]", candidate.getName(), candidate.getSlug());

        String systemPrompt = promptBuilder.buildGenerationSystemPrompt(template);
        String userMessage = promptBuilder.buildGenerationUserMessage(candidate, sourceContext);

        String generatedContent = llmClient.chat(systemPrompt, userMessage);

        KnowledgePage page = new KnowledgePage();
        page.setSlug(candidate.getSlug());
        page.setTitle(candidate.getName());
        page.setCategory(candidate.getCategory());
        page.setContent(generatedContent);
        page.setSourceId(candidate.getSourceId());

        return page;
    }
}
