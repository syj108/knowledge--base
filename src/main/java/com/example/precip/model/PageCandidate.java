package com.example.precip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PageCandidate {

    private String name;
    private String category;
    private String slug;
    private String templateFile;
    private String description;
    private String sourceId;
    private String generatedContent;

    // --- getter/setter ---

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTemplateFile() { return templateFile; }
    public void setTemplateFile(String templateFile) { this.templateFile = templateFile; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getGeneratedContent() { return generatedContent; }
    public void setGeneratedContent(String generatedContent) { this.generatedContent = generatedContent; }
}
