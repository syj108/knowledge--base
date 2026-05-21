package com.example.precip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentState {

    @JsonProperty("kb_version")
    private int kbVersion = 1;

    @JsonProperty("created_at")
    private Instant createdAt = Instant.now();

    @JsonProperty("updated_at")
    private Instant updatedAt = Instant.now();

    private Map<String, SourceRecord> sources = new LinkedHashMap<>();

    private StateStats stats = new StateStats();

    // --- 嵌套类型 ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SourceRecord {
        private String title;
        private String type;
        @JsonProperty("content_hash")
        private String contentHash;
        @JsonProperty("file_path")
        private String filePath;
        @JsonProperty("last_ingested_at")
        private Instant lastIngestedAt;
        private String status;
        @JsonProperty("generated_pages")
        private List<String> generatedPages = new ArrayList<>();
        @JsonProperty("split_suggested")
        private boolean splitSuggested;
        @JsonProperty("assigned_category")
        private String assignedCategory;

        public SourceRecord() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContentHash() { return contentHash; }
        public void setContentHash(String contentHash) { this.contentHash = contentHash; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public Instant getLastIngestedAt() { return lastIngestedAt; }
        public void setLastIngestedAt(Instant lastIngestedAt) { this.lastIngestedAt = lastIngestedAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public List<String> getGeneratedPages() { return generatedPages; }
        public void setGeneratedPages(List<String> generatedPages) { this.generatedPages = generatedPages; }
        public boolean isSplitSuggested() { return splitSuggested; }
        public void setSplitSuggested(boolean splitSuggested) { this.splitSuggested = splitSuggested; }
        public String getAssignedCategory() { return assignedCategory; }
        public void setAssignedCategory(String assignedCategory) { this.assignedCategory = assignedCategory; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StateStats {
        @JsonProperty("total_sources")
        private int totalSources;
        @JsonProperty("total_pages")
        private int totalPages;
        @JsonProperty("total_links")
        private int totalLinks;

        public int getTotalSources() { return totalSources; }
        public void setTotalSources(int totalSources) { this.totalSources = totalSources; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public int getTotalLinks() { return totalLinks; }
        public void setTotalLinks(int totalLinks) { this.totalLinks = totalLinks; }
    }

    // --- getter/setter ---

    public int getKbVersion() { return kbVersion; }
    public void setKbVersion(int kbVersion) { this.kbVersion = kbVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Map<String, SourceRecord> getSources() { return sources; }
    public void setSources(Map<String, SourceRecord> sources) { this.sources = sources; }
    public StateStats getStats() { return stats; }
    public void setStats(StateStats stats) { this.stats = stats; }
}
