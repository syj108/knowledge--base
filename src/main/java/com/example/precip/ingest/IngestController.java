package com.example.precip.ingest;

import com.example.precip.ingest.IngestRequest.*;
import com.example.precip.ingest.parser.CodeRepoParser;
import com.example.precip.ingest.parser.DocumentParser;
import com.example.precip.ingest.parser.TextParser;
import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.kb.StateService;
import com.example.precip.model.AgentState;
import com.example.precip.model.SourceContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/sources")
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final DocumentParser documentParser;
    private final TextParser textParser;
    private final CodeRepoParser codeRepoParser;
    private final SourcePreprocessor preprocessor;
    private final KnowledgeBaseService kbService;
    private final StateService stateService;
    private final ApplicationEventPublisher eventPublisher;

    public IngestController(DocumentParser documentParser,
                            TextParser textParser,
                            CodeRepoParser codeRepoParser,
                            SourcePreprocessor preprocessor,
                            KnowledgeBaseService kbService,
                            StateService stateService,
                            ApplicationEventPublisher eventPublisher) {
        this.documentParser = documentParser;
        this.textParser = textParser;
        this.codeRepoParser = codeRepoParser;
        this.preprocessor = preprocessor;
        this.kbService = kbService;
        this.stateService = stateService;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping(value = "/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SourceResponse> ingestDocument(@RequestPart("file") MultipartFile file) {
        try {
            String title = file.getOriginalFilename();
            byte[] bytes = file.getBytes();

            SourceContent content = documentParser.parse(bytes, title);
            content.setLanguage(preprocessor.detectLanguage(content.getRawText()));

            // 只读归档源文件（原始二进制）
            kbService.saveSourceFile(content.getSourceId(), title, bytes);
            // 保存提取的纯文本（供管道后续读取）
            kbService.saveSourceFile(content.getSourceId(), "_content.txt",
                    content.getRawText().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 更新状态并触发管道
            stateService.addSource(content.getSourceId(), title,
                    content.getType().name(), content.getContentHash(),
                    kbService.getSourceDir(content.getSourceId()).toString());

            eventPublisher.publishEvent(new SourceIngestedEvent(this, content.getSourceId()));

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new SourceResponse(content.getSourceId(), "parsed", "文档已接收，处理中"));
        } catch (Exception e) {
            log.error("文档摄取失败", e);
            return ResponseEntity.internalServerError()
                    .body(new SourceResponse(null, "error", e.getMessage()));
        }
    }

    @PostMapping("/code")
    public ResponseEntity<SourceResponse> ingestCode(@RequestBody CodeIngestRequest request) {
        try {
            Map<String, String> data = Map.of(
                    "gitUrl", request.gitUrl(),
                    "branch", request.branch() != null ? request.branch() : "main"
            );
            SourceContent content = codeRepoParser.parse(data, request.gitUrl());

            // 保存提取的纯文本
            kbService.saveSourceFile(content.getSourceId(), "_content.txt",
                    content.getRawText().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            stateService.addSource(content.getSourceId(), content.getTitle(),
                    content.getType().name(), content.getContentHash(), request.gitUrl());

            eventPublisher.publishEvent(new SourceIngestedEvent(this, content.getSourceId()));

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new SourceResponse(content.getSourceId(), "parsed", "代码仓库已克隆，处理中"));
        } catch (Exception e) {
            log.error("代码仓库摄取失败", e);
            return ResponseEntity.internalServerError()
                    .body(new SourceResponse(null, "error", e.getMessage()));
        }
    }

    @PostMapping("/text")
    public ResponseEntity<SourceResponse> ingestText(@RequestBody TextIngestRequest request) {
        try {
            Map<String, String> data = Map.of(
                    "title", request.title() != null ? request.title() : "未命名文本",
                    "content", request.content(),
                    "language", request.language() != null ? request.language() : ""
            );
            SourceContent content = textParser.parse(data, request.title());

            // 将文本内容保存为源文件（只读归档）
            kbService.saveSourceFile(content.getSourceId(),
                    content.getTitle() + ".txt",
                    request.content().getBytes());
            // 保存提取的纯文本（统一管道读取入口）
            kbService.saveSourceFile(content.getSourceId(), "_content.txt",
                    content.getRawText().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            stateService.addSource(content.getSourceId(), content.getTitle(),
                    content.getType().name(), content.getContentHash(),
                    kbService.getSourceDir(content.getSourceId()).toString());

            eventPublisher.publishEvent(new SourceIngestedEvent(this, content.getSourceId()));

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new SourceResponse(content.getSourceId(), "parsed", "文本已接收，处理中"));
        } catch (Exception e) {
            log.error("文本摄取失败", e);
            return ResponseEntity.internalServerError()
                    .body(new SourceResponse(null, "error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<SourceSummary>> listSources() {
        try {
            AgentState state = stateService.getState();
            List<SourceSummary> list = new ArrayList<>();
            state.getSources().forEach((id, record) ->
                    list.add(new SourceSummary(id, record.getTitle(), record.getType(), record.getStatus())));
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Map<String, String>> getSourceContent(@PathVariable String id) {
        try {
            Path sourceDir = kbService.getSourceDir(id);
            if (!Files.isDirectory(sourceDir)) {
                return ResponseEntity.notFound().build();
            }
            // 优先读取提取的纯文本
            Path contentFile = sourceDir.resolve("_content.txt");
            if (Files.exists(contentFile)) {
                String content = Files.readString(contentFile, StandardCharsets.UTF_8);
                return ResponseEntity.ok(Map.of("content", content, "fileName", "_content.txt"));
            }
            // 回退：尝试读取非元数据文件
            try (Stream<Path> files = Files.list(sourceDir)) {
                Path firstFile = files
                        .filter(Files::isRegularFile)
                        .filter(p -> !p.getFileName().toString().startsWith("_"))
                        .findFirst().orElse(null);
                if (firstFile == null) {
                    return ResponseEntity.ok(Map.of("content", "", "fileName", ""));
                }
                String content = Files.readString(firstFile, StandardCharsets.UTF_8);
                String fileName = firstFile.getFileName().toString();
                return ResponseEntity.ok(Map.of("content", content, "fileName", fileName));
            }
        } catch (Exception e) {
            log.error("读取源文档内容失败: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<SourceStatusResponse> getStatus(@PathVariable String id) {
        try {
            var record = stateService.getSourceRecord(id);
            if (record == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(new SourceStatusResponse(
                    id, record.getStatus(), record.isSplitSuggested(), record.getGeneratedPages()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
