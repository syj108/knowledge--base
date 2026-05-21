package com.example.precip.ingest;

import org.springframework.context.ApplicationEvent;

public class SourceIngestedEvent extends ApplicationEvent {

    private final String sourceId;

    public SourceIngestedEvent(Object source, String sourceId) {
        super(source);
        this.sourceId = sourceId;
    }

    public String getSourceId() {
        return sourceId;
    }
}
