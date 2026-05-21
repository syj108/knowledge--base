package com.example.precip.pipeline;

import com.example.precip.ingest.SourcePreprocessor;
import com.example.precip.llm.LlmClient;
import com.example.precip.llm.PromptBuilder;
import com.example.precip.model.PageCandidate;
import com.example.precip.model.SourceContent;
import com.example.precip.model.TemplateDef;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pass 0：从源内容中提取知识候选列表。
 */
@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final SourcePreprocessor preprocessor;

    public ExtractionService(LlmClient llmClient, PromptBuilder promptBuilder,
                             SourcePreprocessor preprocessor) {
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.preprocessor = preprocessor;
    }

    public List<PageCandidate> extractCandidates(SourceContent source,
                                                  Map<String, TemplateDef> templates,
                                                  String assignedCategory) {
        String systemPrompt = promptBuilder.buildExtractionSystemPrompt(templates, assignedCategory);
        String truncatedText = preprocessor.truncate(source.getRawText());

        SourceContent truncatedSource = new SourceContent();
        truncatedSource.setSourceId(source.getSourceId());
        truncatedSource.setType(source.getType());
        truncatedSource.setTitle(source.getTitle());
        truncatedSource.setLanguage(source.getLanguage());
        truncatedSource.setRawText(truncatedText);

        String userMessage = promptBuilder.buildExtractionUserMessage(truncatedSource);

        try {
            List<PageCandidate> candidates = llmClient.chatForJson(
                    systemPrompt, userMessage, new TypeReference<>() {});

            // 设置 sourceId；若有指定分类则强制覆盖
            for (PageCandidate c : candidates) {
                c.setSourceId(source.getSourceId());
                if (assignedCategory != null && !assignedCategory.isBlank()) {
                    c.setCategory(assignedCategory);
                    // 修正 slug 前缀
                    String slugName = c.getSlug();
                    int slash = slugName.indexOf('/');
                    if (slash >= 0) {
                        slugName = slugName.substring(slash + 1);
                    }
                    c.setSlug(assignedCategory + "/" + slugName);
                }
            }

            log.info("从源 [{}] 提取了 {} 个候选", source.getTitle(), candidates.size());
            return candidates;
        } catch (Exception e) {
            log.warn("候选提取失败（源: {}），返回空列表: {}", source.getTitle(), e.getMessage());
            return new ArrayList<>();
        }
    }
}
