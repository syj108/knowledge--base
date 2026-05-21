package com.example.precip.ingest;

import java.util.List;

public class IngestRequest {

    public record CodeIngestRequest(String gitUrl, String branch) {}

    public record TextIngestRequest(String title, String content, String language) {}

    public record SourceResponse(String sourceId, String status, String message) {}

    public record SourceSummary(String sourceId, String title, String type, String status) {}

    public record SourceStatusResponse(String sourceId, String status, boolean splitSuggested,
                                       List<String> generatedPages) {}
}
