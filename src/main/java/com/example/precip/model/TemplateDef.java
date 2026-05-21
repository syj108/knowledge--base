package com.example.precip.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板定义。
 *
 * @param fileName     模板文件名，如 "knowledgeBaseOutput.md"
 * @param categoryName 分类名（kebab-case，去掉 .md 后缀），如 "knowledge-base-output"
 * @param content      模板 Markdown 全文
 * @param sections     按 H1 标题拆分的段落标题列表
 */
public record TemplateDef(
        String fileName,
        String categoryName,
        String content,
        List<String> sections
) {

    private static final Pattern H1_PATTERN = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);

    /**
     * 从文件名生成 kebab-case 分类名。
     * 如：knowledgeBaseOutput.md → knowledge-base-output
     */
    public static String toCategoryName(String fileName) {
        String stem = fileName.endsWith(".md")
                ? fileName.substring(0, fileName.length() - 3)
                : fileName;
        // 驼峰 → kebab-case
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stem.length(); i++) {
            char c = stem.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append('-');
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    /**
     * 从模板内容中提取所有 H1 标题。
     */
    public static List<String> extractH1Sections(String content) {
        List<String> sections = new ArrayList<>();
        Matcher matcher = H1_PATTERN.matcher(content);
        while (matcher.find()) {
            sections.add(matcher.group(1).trim());
        }
        return sections;
    }

    /**
     * 按 H1 标题将模板内容拆分为多个段落。
     * 返回列表的每个元素对应一个 H1 段落（含标题行及其下方内容）。
     */
    public List<String> splitBySections() {
        List<String> parts = new ArrayList<>();
        Matcher matcher = H1_PATTERN.matcher(content);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        if (starts.isEmpty()) {
            parts.add(content);
            return parts;
        }
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : content.length();
            parts.add(content.substring(start, end).trim());
        }
        return parts;
    }
}
