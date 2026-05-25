package com.example.precip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * 去重审核记录：当管道检测到新页面与已有页面高度相似时，暂存待用户决策。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DedupReview {

    private String id;
    private String sourceId;

    // 新生成的页面信息
    private String newPageTitle;
    private String newPageCategory;
    private String newPageSlug;
    private String newPageSummary;
    private String newPageContent;

    // 匹配到的已有页面信息
    private String existingPageSlug;
    private String existingPageTitle;
    private String existingPageCategory;
    private String existingPageSummary;

    // LLM 建议
    private String suggestion;   // "merge" / "uncertain"
    private String reason;

    // 状态
    private String status;       // "pending" / "merged" / "separated"
    private Instant createdAt;

    public DedupReview() {}

    // --- getter/setter ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getNewPageTitle() { return newPageTitle; }
    public void setNewPageTitle(String newPageTitle) { this.newPageTitle = newPageTitle; }
    public String getNewPageCategory() { return newPageCategory; }
    public void setNewPageCategory(String newPageCategory) { this.newPageCategory = newPageCategory; }
    public String getNewPageSlug() { return newPageSlug; }
    public void setNewPageSlug(String newPageSlug) { this.newPageSlug = newPageSlug; }
    public String getNewPageSummary() { return newPageSummary; }
    public void setNewPageSummary(String newPageSummary) { this.newPageSummary = newPageSummary; }
    public String getNewPageContent() { return newPageContent; }
    public void setNewPageContent(String newPageContent) { this.newPageContent = newPageContent; }

    public String getExistingPageSlug() { return existingPageSlug; }
    public void setExistingPageSlug(String existingPageSlug) { this.existingPageSlug = existingPageSlug; }
    public String getExistingPageTitle() { return existingPageTitle; }
    public void setExistingPageTitle(String existingPageTitle) { this.existingPageTitle = existingPageTitle; }
    public String getExistingPageCategory() { return existingPageCategory; }
    public void setExistingPageCategory(String existingPageCategory) { this.existingPageCategory = existingPageCategory; }
    public String getExistingPageSummary() { return existingPageSummary; }
    public void setExistingPageSummary(String existingPageSummary) { this.existingPageSummary = existingPageSummary; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
