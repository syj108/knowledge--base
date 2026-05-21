package com.example.precip.model;

import java.time.Instant;

public class KnowledgePage {

    private String slug;
    private String title;
    private String category;
    private String content;
    private String sourceId;
    private Instant generatedAt = Instant.now();
    private boolean isNew = true;

    // --- getter/setter ---

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public Instant getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
    public boolean isNew() { return isNew; }
    public void setNew(boolean aNew) { isNew = aNew; }
}
