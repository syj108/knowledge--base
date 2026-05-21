package com.example.precip.llm;

import com.example.precip.model.PageCandidate;
import com.example.precip.model.SourceContent;
import com.example.precip.model.TemplateDef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Prompt 集中管理。所有 LLM 调用的 prompt 在此组装，避免散落在各服务中。
 */
@Component
public class PromptBuilder {

    // --- Pass 0: 候选提取 ---

    public String buildExtractionSystemPrompt(Map<String, TemplateDef> templates,
                                              String assignedCategory) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是一个知识沉淀专家。请分析提供的源内容，识别其中值得沉淀为独立知识页面的知识点。

                目标模板结构（基于 templates/ 目录中的文件）：
                """);

        for (var entry : templates.entrySet()) {
            TemplateDef def = entry.getValue();
            sb.append("- ").append(entry.getKey())
                    .append(" (").append(def.fileName()).append("): ");
            if (!def.sections().isEmpty()) {
                sb.append("包含章节 ").append(String.join("、", def.sections()));
            }
            sb.append("\n");
        }

        sb.append("""

                请返回严格的 JSON 数组，描述从源内容中可以提取哪些知识候选：
                [{
                  "name": "产品或技术名称（从内容中识别核心产品/系统/技术的名称，作为知识页面标题）",
                  "category": "内容主题分类（按知识内容的核心主题归类，例如 ecs、srs、network、database 等）",
                  "slug": "分类名/kebab-case-标题",
                  "templateFile": "模板文件名.md",
                  "description": "一句话描述该知识点"
                }]

                规则：
                1. 默认为一个源生成一个完整的知识文档候选（包含模板的所有章节）
                2. name 必须是从文档内容中识别出的核心产品名、系统名或技术名称，例如 "5G NR射频指纹识别系统"、"ECS弹性云服务器"。不要使用模板章节名（如"产品对外文档"）作为 name
                """);

        if (assignedCategory != null && !assignedCategory.isBlank()) {
            sb.append("3. category 字段必须使用: \"").append(assignedCategory)
                    .append("\"，不要自行决定分类。slug 前缀也必须使用该分类名\n");
        } else {
            sb.append("3. category 必须按内容主题归类，不是模板名称。例如：关于 ECS 的内容归为 \"ecs\"，关于 SRS 的归为 \"srs\"，关于数据库的归为 \"database\"。使用简短的英文小写 kebab-case 作为分类名\n");
        }

        sb.append("""
                4. slug 使用全小写 kebab-case，格式为 "分类名/文档标题"
                5. 只返回 JSON 数组，不要有任何其他解释文字
                6. 如果源内容主题聚焦，只返回一个候选即可
                """);

        return sb.toString();
    }

    public String buildExtractionUserMessage(SourceContent source) {
        StringBuilder sb = new StringBuilder();
        sb.append("源文档标题: ").append(source.getTitle()).append("\n");
        sb.append("源文档类型: ").append(source.getType()).append("\n");
        if (source.getLanguage() != null) {
            sb.append("语言: ").append(source.getLanguage()).append("\n");
        }
        sb.append("\n--- 源内容 ---\n\n");
        sb.append(source.getRawText());
        return sb.toString();
    }

    // --- Pass 1: 模板填充 ---

    public String buildGenerationSystemPrompt(TemplateDef template) {
        return """
                你是一个知识沉淀专家。请根据以下模板格式，将源内容整理、提取、归纳为结构化的知识文档。

                注意：源内容是原始素材，不一定直接包含模板要求的内容。你需要从中理解、提炼、总结，
                最终生成符合模板格式的文档。源内容保持只读，不要修改或删除。

                模板格式（请严格遵循此结构）：
                """ + template.content() + """

                规则：
                1. 用从源内容中提炼的信息填充模板各章节
                2. 如果源内容中没有某章节对应的信息，删除该章节（包含标题）
                3. 使用 [[目录名/文件名|显示名]] 语法引用其他相关知识页面
                4. 对于告警表格等结构化内容，尽量保留 Markdown 表格格式
                5. 输出纯 Markdown，不要添加任何前言或后记
                6. 不要输出一级标题（# 开头），系统会自动添加文档标题。模板中的顶级章节请使用二级标题（##）
                """;
    }

    public String buildGenerationUserMessage(PageCandidate candidate, String sourceContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下知识点生成文档：\n\n");
        sb.append("标题: ").append(candidate.getName()).append("\n");
        sb.append("分类: ").append(candidate.getCategory()).append("\n");
        sb.append("描述: ").append(candidate.getDescription()).append("\n");
        sb.append("\n--- 源内容（提炼素材，只读） ---\n\n");
        sb.append(sourceContext);
        return sb.toString();
    }

    // --- 去重判定 ---

    public String buildDeduplicationPrompt(String existingSummary, String newSummary) {
        return """
                现有知识页面摘要（前 500 字）：
                """ + existingSummary + """

                新生成的知识页面摘要（前 500 字）：
                """ + newSummary + """

                请判断这两个页面是否描述同一知识点。返回以下 JSON 之一：
                {"decision": "merge"}         // 完全是同一实体，合并内容
                {"decision": "new_version"}   // 同一实体但新版本覆盖旧版本
                {"decision": "keep_both"}     // 不同实体，恰好同名，保留两者

                只返回 JSON，不要有其他文字。
                """;
    }

    // --- 索引简介生成 ---

    public String buildIndexIntroPrompt(Map<String, List<String>> categorizedPages) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为以下知识库生成一段简短的中文简介（2-3 句话），描述知识库的主题和规模：\n\n");
        for (var entry : categorizedPages.entrySet()) {
            sb.append("- ").append(entry.getKey())
                    .append(": ").append(entry.getValue().size()).append(" 篇文档\n");
        }
        sb.append("\n只返回简介文字，不要有标题或格式标记。");
        return sb.toString();
    }
}
