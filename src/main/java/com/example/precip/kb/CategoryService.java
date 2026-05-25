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

    public CategoryDef findByKey(String key) {
        return listCategories().stream()
                .filter(c -> c.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    public synchronized CategoryDef updateCategory(String oldKey, String newKey,
                                                    String newName, String newDescription) throws IOException {
        List<CategoryDef> categories = new ArrayList<>(doLoad());
        int idx = -1;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).key().equals(oldKey)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            throw new IllegalArgumentException("分类不存在: " + oldKey);
        }
        CategoryDef old = categories.get(idx);
        String effectiveKey = (newKey != null && !newKey.isBlank()) ? newKey : old.key();
        String effectiveName = (newName != null && !newName.isBlank()) ? newName : old.name();
        String effectiveDesc = (newDescription != null) ? newDescription : old.description();

        if (!effectiveKey.equals(oldKey)) {
            boolean conflict = categories.stream().anyMatch(c -> c.key().equals(effectiveKey));
            if (conflict) {
                throw new IllegalArgumentException("分类 key 已存在: " + effectiveKey);
            }
        }

        CategoryDef updated = new CategoryDef(effectiveKey, effectiveName, effectiveDesc);
        categories.set(idx, updated);
        doSave(categories);
        cache.set(categories);
        log.info("更新类别: {} → {} [{}]", oldKey, effectiveKey, effectiveName);
        return updated;
    }

    public synchronized void deleteCategory(String key) throws IOException {
        List<CategoryDef> categories = new ArrayList<>(doLoad());
        boolean removed = categories.removeIf(c -> c.key().equals(key));
        if (!removed) {
            throw new IllegalArgumentException("分类不存在: " + key);
        }
        doSave(categories);
        cache.set(categories);
        log.info("删除类别: {}", key);
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
