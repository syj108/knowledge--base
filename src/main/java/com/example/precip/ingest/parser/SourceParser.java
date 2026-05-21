package com.example.precip.ingest.parser;

import com.example.precip.model.SourceContent;

public interface SourceParser {

    boolean supports(SourceContent.SourceType type);

    SourceContent parse(Object input, String title) throws Exception;
}
