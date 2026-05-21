package com.example.precip.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceContent {

    private String sourceId;
    private SourceType type;
    private String title;
    private String language;
    private String rawText;
    private List<CodeBlock> codeBlocks = new ArrayList<>();
    private Map<String, String> metadata = new HashMap<>();
    private String contentHash;

    public enum SourceType {
        DOCUMENT, CODE_REPO, FREE_TEXT
    }

    public record CodeBlock(String filePath, String language, String content) {}

    // --- getter/setter ---

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public SourceType getType() { return type; }
    public void setType(SourceType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public List<CodeBlock> getCodeBlocks() { return codeBlocks; }
    public void setCodeBlocks(List<CodeBlock> codeBlocks) { this.codeBlocks = codeBlocks; }
    public Map<String, String> getMetadata() { return metadata; }
    public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
}
