package com.example.precip.kb;

import com.example.precip.config.KnowledgeBaseConfig;
import com.example.precip.model.CategoryDef;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 类别管理服务：负责 categories.json 的读写。
 */
@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final KnowledgeBaseConfig config;
    private final ObjectMapper objectMapper;
    private final AtomicReference<List<CategoryDef>> cache = new AtomicReference<>();

    public CategoryService(KnowledgeBaseConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
    }

    public List<CategoryDef> listCategories() {
        List<CategoryDef> cached = cache.get();
        if (cached != null) {
            return cached;
        }
        List<CategoryDef> result = doLoad();
        cache.set(result);
        return result;
    }

    public synchronized CategoryDef addCategory(String key, String name, String description) throws IOException {
        List<CategoryDef> categories = new ArrayList<>(doLoad());
        CategoryDef newCat = new CategoryDef(key, name, description);
        categories.add(newCat);
        doSave(categories);
        cache.set(categories);
        log.info("新增类别: {} [{}]", name, key);
        return newCat;
    }

    private List<CategoryDef> doLoad() {
        Path file = config.categoriesFile();
        if (Files.notExists(file)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(file.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            log.error("读取 categories.json 失败", e);
            return new ArrayList<>();
        }
    }

    private void doSave(List<CategoryDef> categories) throws IOException {
        Path file = config.categoriesFile();
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(categories);
        Files.writeString(tmp, json, StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
}
