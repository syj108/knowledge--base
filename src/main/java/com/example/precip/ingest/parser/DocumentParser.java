package com.example.precip.ingest.parser;

import com.example.precip.model.SourceContent;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class DocumentParser implements SourceParser {

    @Override
    public boolean supports(SourceContent.SourceType type) {
        return type == SourceContent.SourceType.DOCUMENT;
    }

    /**
     * 解析文档文件。
     * @param input byte[] 文件内容
     * @param title 文档标题（可为文件名）
     */
    @Override
    public SourceContent parse(Object input, String title) throws Exception {
        byte[] bytes = (byte[]) input;

        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();

        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
            parser.parse(is, handler, metadata);
        }

        String rawText = handler.toString();
        String hash = sha256(rawText);

        SourceContent content = new SourceContent();
        content.setSourceId(UUID.randomUUID().toString());
        content.setType(SourceContent.SourceType.DOCUMENT);
        content.setTitle(title);
        content.setRawText(rawText);
        content.setContentHash(hash);

        // 提取元数据
        String author = metadata.get(TikaCoreProperties.CREATOR);
        if (author != null) content.getMetadata().put("author", author);
        String contentType = metadata.get(Metadata.CONTENT_TYPE);
        if (contentType != null) content.getMetadata().put("contentType", contentType);

        return content;
    }

    private String sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return "sha256:" + HexFormat.of().formatHex(hash);
    }
}
