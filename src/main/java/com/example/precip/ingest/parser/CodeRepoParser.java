package com.example.precip.ingest.parser;

import com.example.precip.model.SourceContent;
import com.example.precip.model.SourceContent.CodeBlock;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

/**
 * MVP 实现：JGit 克隆 + 纯文件内容读取，不做 AST 解析。
 */
@Component
public class CodeRepoParser implements SourceParser {

    private static final Logger log = LoggerFactory.getLogger(CodeRepoParser.class);

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".java", ".py", ".go", ".ts", ".tsx", ".js", ".jsx",
            ".md", ".yml", ".yaml", ".json", ".xml", ".sql",
            ".rs", ".rb", ".kt", ".scala", ".sh"
    );

    private static final Set<String> SKIP_DIRS = Set.of(
            ".git", "node_modules", "target", "build", "dist",
            "vendor", ".idea", ".vscode", "__pycache__", ".gradle"
    );

    @Override
    public boolean supports(SourceContent.SourceType type) {
        return type == SourceContent.SourceType.CODE_REPO;
    }

    /**
     * 解析代码仓库。
     * @param input Map with keys: "gitUrl", "branch"(可选)
     * @param title 仓库标题
     */
    @Override
    @SuppressWarnings("unchecked")
    public SourceContent parse(Object input, String title) throws Exception {
        Map<String, String> data = (Map<String, String>) input;
        String gitUrl = data.get("gitUrl");
        String branch = data.getOrDefault("branch", "main");

        Path tempDir = Files.createTempDirectory("precip-clone-");
        try {
            log.info("克隆仓库: {} (分支: {})", gitUrl, branch);
            Git.cloneRepository()
                    .setURI(gitUrl)
                    .setBranch(branch)
                    .setDirectory(tempDir.toFile())
                    .call()
                    .close();

            return buildSourceContent(tempDir, title != null ? title : gitUrl);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private SourceContent buildSourceContent(Path repoDir, String title) throws Exception {
        StringBuilder rawText = new StringBuilder();
        List<CodeBlock> codeBlocks = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(repoDir)) {
            walk.filter(Files::isRegularFile)
                    .filter(this::isCodeFile)
                    .sorted()
                    .forEach(path -> {
                        try {
                            String relativePath = repoDir.relativize(path).toString();
                            if (shouldSkip(relativePath)) return;

                            String fileContent = Files.readString(path, StandardCharsets.UTF_8);
                            String extension = getExtension(path.getFileName().toString());
                            String lang = extensionToLanguage(extension);

                            rawText.append("--- ").append(relativePath).append(" ---\n");
                            rawText.append(fileContent).append("\n\n");

                            codeBlocks.add(new CodeBlock(relativePath, lang, fileContent));
                        } catch (IOException e) {
                            log.warn("读取文件失败: {}", path, e);
                        }
                    });
        }

        String text = rawText.toString();
        SourceContent content = new SourceContent();
        content.setSourceId(UUID.randomUUID().toString());
        content.setType(SourceContent.SourceType.CODE_REPO);
        content.setTitle(title);
        content.setRawText(text);
        content.setCodeBlocks(codeBlocks);
        content.setLanguage(TextParser.detectLanguage(text));
        content.setContentHash(sha256(text));

        return content;
    }

    private boolean isCodeFile(Path path) {
        String name = path.getFileName().toString();
        return CODE_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private boolean shouldSkip(String relativePath) {
        for (String skip : SKIP_DIRS) {
            if (relativePath.startsWith(skip + "/") || relativePath.contains("/" + skip + "/")) {
                return true;
            }
        }
        return false;
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }

    private String extensionToLanguage(String ext) {
        return switch (ext) {
            case ".java" -> "java";
            case ".py" -> "python";
            case ".go" -> "go";
            case ".ts", ".tsx" -> "typescript";
            case ".js", ".jsx" -> "javascript";
            case ".md" -> "markdown";
            case ".yml", ".yaml" -> "yaml";
            case ".json" -> "json";
            case ".xml" -> "xml";
            case ".sql" -> "sql";
            case ".rs" -> "rust";
            case ".rb" -> "ruby";
            case ".kt" -> "kotlin";
            case ".scala" -> "scala";
            case ".sh" -> "shell";
            default -> "text";
        };
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.delete(d);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("清理临时目录失败: {}", dir, e);
        }
    }

    private String sha256(String text) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return "sha256:" + HexFormat.of().formatHex(hash);
    }
}
