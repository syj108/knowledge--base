package com.example.precip.ingest;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SourcePreprocessor {

    private static final int MAX_CHARS = 30000;
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,2}\\s+.+$", Pattern.MULTILINE);

    /**
     * 截断超长文本，按段落边界截断。
     */
    public String truncate(String text) {
        if (text == null || text.length() <= MAX_CHARS) {
            return text;
        }
        int cutPoint = text.lastIndexOf("\n\n", MAX_CHARS);
        if (cutPoint <= 0) {
            cutPoint = text.lastIndexOf("\n", MAX_CHARS);
        }
        if (cutPoint <= 0) {
            cutPoint = MAX_CHARS;
        }
        return text.substring(0, cutPoint) + "\n\n[... 内容已截断 ...]";
    }

    /**
     * 按 Markdown H1/H2 标题分节。
     */
    public List<String> splitByHeadings(String text) {
        List<String> sections = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return sections;
        }

        Matcher matcher = HEADING_PATTERN.matcher(text);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(matcher.start());
        }

        if (starts.isEmpty()) {
            sections.add(text);
            return sections;
        }

        // 标题前面的内容作为第一段（如果有的话）
        if (starts.get(0) > 0) {
            String preamble = text.substring(0, starts.get(0)).trim();
            if (!preamble.isEmpty()) {
                sections.add(preamble);
            }
        }

        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : text.length();
            sections.add(text.substring(start, end).trim());
        }

        return sections;
    }

    /**
     * 语言检测：中文字符占比 > 20% → "zh"，否则 "en"。
     */
    public String detectLanguage(String text) {
        if (text == null || text.isEmpty()) return "en";
        long chineseCount = text.codePoints()
                .filter(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN)
                .count();
        double ratio = (double) chineseCount / text.codePoints().count();
        return ratio > 0.2 ? "zh" : "en";
    }
}
