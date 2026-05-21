package com.example.precip.ingest.parser;

import com.example.precip.model.SourceContent;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Component
public class TextParser implements SourceParser {

    @Override
    public boolean supports(SourceContent.SourceType type) {
        return type == SourceContent.SourceType.FREE_TEXT;
    }

    /**
     * 解析自由文本输入。
     * @param input Map with keys: "title", "content", "language"(可选)
     * @param title 文本标题
     */
    @Override
    @SuppressWarnings("unchecked")
    public SourceContent parse(Object input, String title) throws Exception {
        Map<String, String> data = (Map<String, String>) input;

        String rawText = data.getOrDefault("content", "");
        String language = data.get("language");
        if (language == null || language.isBlank()) {
            language = detectLanguage(rawText);
        }

        SourceContent content = new SourceContent();
        content.setSourceId(UUID.randomUUID().toString());
        content.setType(SourceContent.SourceType.FREE_TEXT);
        content.setTitle(title != null ? title : data.getOrDefault("title", "未命名文本"));
        content.setRawText(rawText);
        content.setLanguage(language);
        content.setContentHash(sha256(rawText));

        return content;
    }

    static String detectLanguage(String text) {
        if (text == null || text.isEmpty()) return "en";
        long chineseCount = text.codePoints()
                .filter(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN)
                .count();
        double ratio = (double) chineseCount / text.codePoints().count();
        return ratio > 0.2 ? "zh" : "en";
    }

    private String sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return "sha256:" + HexFormat.of().formatHex(hash);
    }
}
