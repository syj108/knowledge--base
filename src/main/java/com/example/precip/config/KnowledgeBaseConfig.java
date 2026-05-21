package com.example.precip.config;

import com.example.precip.model.AgentState;
import com.example.precip.model.GraphData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Component
@ConfigurationProperties(prefix = "precip.kb")
public class KnowledgeBaseConfig {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseConfig.class);

    private Path baseDir = Path.of("./knowledge-base");
    private boolean autoGitCommit = true;

    private final ObjectMapper objectMapper;

    public KnowledgeBaseConfig() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void initialize() throws IOException {
        baseDir = baseDir.toAbsolutePath().normalize();
        log.info("初始化知识库目录: {}", baseDir);

        // 创建目录骨架
        Files.createDirectories(templatesDir());
        Files.createDirectories(pagesDir());
        Files.createDirectories(sourcesDir());

        // 初始化 graph.json
        if (Files.notExists(graphFile())) {
            GraphData emptyGraph = new GraphData();
            objectMapper.writeValue(graphFile().toFile(), emptyGraph);
            log.info("已创建 graph.json");
        }

        // 初始化 state.json
        if (Files.notExists(stateFile())) {
            AgentState initialState = new AgentState();
            objectMapper.writeValue(stateFile().toFile(), initialState);
            log.info("已创建 state.json");
        }

        // 复制默认模板到知识库
        copyDefaultTemplateIfEmpty();

        // 初始化 Git 仓库
        initGitIfNeeded();
    }

    private void copyDefaultTemplateIfEmpty() throws IOException {
        try (var stream = Files.list(templatesDir())) {
            if (stream.findAny().isEmpty()) {
                ClassPathResource resource = new ClassPathResource("templates/knowledgeBaseOutput.md");
                if (resource.exists()) {
                    Path target = templatesDir().resolve("knowledgeBaseOutput.md");
                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    log.info("已复制默认模板到: {}", target);
                }
            }
        }
    }

    private void initGitIfNeeded() {
        if (Files.notExists(baseDir.resolve(".git"))) {
            try {
                Git.init().setDirectory(baseDir.toFile()).call().close();
                log.info("已在知识库目录初始化 Git 仓库");
            } catch (Exception e) {
                log.warn("Git 初始化失败，跳过: {}", e.getMessage());
            }
        }
    }

    // --- 路径访问器 ---

    public Path getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(Path baseDir) {
        this.baseDir = baseDir;
    }

    public boolean isAutoGitCommit() {
        return autoGitCommit;
    }

    public void setAutoGitCommit(boolean autoGitCommit) {
        this.autoGitCommit = autoGitCommit;
    }

    public Path templatesDir() {
        return baseDir.resolve("templates");
    }

    public Path pagesDir() {
        return baseDir.resolve("pages");
    }

    public Path sourcesDir() {
        return baseDir.resolve("sources");
    }

    public Path graphFile() {
        return baseDir.resolve("graph.json");
    }

    public Path stateFile() {
        return baseDir.resolve("state.json");
    }

    public Path indexFile() {
        return baseDir.resolve("index.md");
    }
}
