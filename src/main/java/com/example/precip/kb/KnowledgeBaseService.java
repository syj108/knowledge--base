package com.example.precip.kb;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.model.AgentState;
import com.example.precip.model.GraphData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 知识库文件系统读写门面。
 * 所有对知识库目录的文件操作集中在此服务中，其他服务不直接操作 Path。
 */
@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseConfig config;
    private final ObjectMapper objectMapper;

    public KnowledgeBaseService(KnowledgeBaseConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // --- 页面操作 ---

    public void writePage(String slug, String content) throws IOException {
        Path target = config.pagesDir().resolve(slug + ".md");
        Files.createDirectories(target.getParent());
        atomicWrite(target, content);
    }

    public String readPage(String slug) throws IOException {
        Path file = config.pagesDir().resolve(slug + ".md");
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public boolean pageExists(String slug) {
        return Files.exists(config.pagesDir().resolve(slug + ".md"));
    }

    public List<String> allPageSlugs() throws IOException {
        List<String> slugs = new ArrayList<>();
        Path pagesDir = config.pagesDir();
        if (Files.notExists(pagesDir)) {
            return slugs;
        }
        try (Stream<Path> walk = Files.walk(pagesDir)) {
            walk.filter(p -> p.toString().endsWith(".md"))
                    .forEach(p -> {
                        String relative = pagesDir.relativize(p).toString();
                        // 去掉 .md 后缀，统一使用 / 分隔符
                        String slug = relative.substring(0, relative.length() - 3)
                                .replace(FileSystems.getDefault().getSeparator(), "/");
                        slugs.add(slug);
                    });
        }
        return slugs;
    }

    // --- 源文档存储（只读归档）---

    public Path saveSourceFile(String sourceId, String fileName, byte[] content) throws IOException {
        Path sourceDir = config.sourcesDir().resolve(sourceId);
        Files.createDirectories(sourceDir);
        Path target = sourceDir.resolve(fileName);
        Files.write(target, content);
        return target;
    }

    public Path getSourceDir(String sourceId) {
        return config.sourcesDir().resolve(sourceId);
    }

    // --- graph.json ---

    public GraphData readGraphData() throws IOException {
        return objectMapper.readValue(config.graphFile().toFile(), GraphData.class);
    }

    public void writeGraphData(GraphData graphData) throws IOException {
        atomicWrite(config.graphFile(), objectMapper.writeValueAsString(graphData));
    }

    // --- state.json ---

    public AgentState readState() throws IOException {
        return objectMapper.readValue(config.stateFile().toFile(), AgentState.class);
    }

    public void writeState(AgentState state) throws IOException {
        atomicWrite(config.stateFile(), objectMapper.writeValueAsString(state));
    }

    // --- index.md ---

    public void writeIndex(String content) throws IOException {
        atomicWrite(config.indexFile(), content);
    }

    // --- 原子写入 ---

    private void atomicWrite(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
