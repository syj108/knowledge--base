package com.example.precip.kb;

import com.example.precip.model.AgentState;
import com.example.precip.model.AgentState.SourceRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * state.json 生命周期管理。
 * 提供对源记录状态的原子更新操作。
 */
@Service
public class StateService {

    private static final Logger log = LoggerFactory.getLogger(StateService.class);

    private final KnowledgeBaseService kbService;

    public StateService(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    public synchronized void addSource(String sourceId, String title, String type,
                                       String contentHash, String filePath) throws IOException {
        addSource(sourceId, title, type, contentHash, filePath, null);
    }

    public synchronized void addSource(String sourceId, String title, String type,
                                       String contentHash, String filePath,
                                       String assignedCategory) throws IOException {
        AgentState state = kbService.readState();
        SourceRecord record = new SourceRecord();
        record.setTitle(title);
        record.setType(type);
        record.setContentHash(contentHash);
        record.setFilePath(filePath);
        record.setLastIngestedAt(Instant.now());
        record.setStatus("parsed");
        record.setAssignedCategory(assignedCategory);
        state.getSources().put(sourceId, record);
        state.setUpdatedAt(Instant.now());
        state.getStats().setTotalSources(state.getSources().size());
        kbService.writeState(state);
        log.info("已添加源记录: {} [{}]", sourceId, title);
    }

    public synchronized void updateSourceStatus(String sourceId, String status) throws IOException {
        AgentState state = kbService.readState();
        SourceRecord record = state.getSources().get(sourceId);
        if (record != null) {
            record.setStatus(status);
            state.setUpdatedAt(Instant.now());
            kbService.writeState(state);
            log.debug("源 {} 状态更新为: {}", sourceId, status);
        }
    }

    public synchronized void markCompleted(String sourceId, List<String> generatedPages) throws IOException {
        AgentState state = kbService.readState();
        SourceRecord record = state.getSources().get(sourceId);
        if (record != null) {
            record.setStatus("completed");
            record.setGeneratedPages(generatedPages);
            record.setLastIngestedAt(Instant.now());
            state.setUpdatedAt(Instant.now());
            updateStats(state);
            kbService.writeState(state);
            log.info("源 {} 处理完成，生成 {} 个页面", sourceId, generatedPages.size());
        }
    }

    public synchronized void markFailed(String sourceId) throws IOException {
        updateSourceStatus(sourceId, "failed");
        log.warn("源 {} 处理失败", sourceId);
    }

    public synchronized void markSplitSuggested(String sourceId) throws IOException {
        AgentState state = kbService.readState();
        SourceRecord record = state.getSources().get(sourceId);
        if (record != null) {
            record.setSplitSuggested(true);
            state.setUpdatedAt(Instant.now());
            kbService.writeState(state);
            log.info("源 {} 建议拆分", sourceId);
        }
    }

    public synchronized void replaceSlugPrefix(String oldPrefix, String newPrefix) throws IOException {
        AgentState state = kbService.readState();
        for (SourceRecord record : state.getSources().values()) {
            List<String> pages = record.getGeneratedPages();
            if (pages != null) {
                pages.replaceAll(slug -> {
                    if (slug.startsWith(oldPrefix + "/")) {
                        return newPrefix + slug.substring(oldPrefix.length());
                    }
                    return slug;
                });
            }
        }
        state.setUpdatedAt(Instant.now());
        kbService.writeState(state);
    }

    public synchronized void movePageSlug(String oldSlug, String newSlug) throws IOException {
        AgentState state = kbService.readState();
        for (SourceRecord record : state.getSources().values()) {
            List<String> pages = record.getGeneratedPages();
            if (pages != null) {
                pages.replaceAll(slug -> slug.equals(oldSlug) ? newSlug : slug);
            }
        }
        state.setUpdatedAt(Instant.now());
        kbService.writeState(state);
    }

    public synchronized void removePageSlugs(List<String> slugsToRemove) throws IOException {
        AgentState state = kbService.readState();
        for (SourceRecord record : state.getSources().values()) {
            List<String> pages = record.getGeneratedPages();
            if (pages != null) {
                pages.removeAll(slugsToRemove);
            }
        }
        state.setUpdatedAt(Instant.now());
        updateStats(state);
        kbService.writeState(state);
    }

    public AgentState getState() throws IOException {
        return kbService.readState();
    }

    public SourceRecord getSourceRecord(String sourceId) throws IOException {
        return kbService.readState().getSources().get(sourceId);
    }

    private void updateStats(AgentState state) {
        int totalPages = state.getSources().values().stream()
                .mapToInt(r -> r.getGeneratedPages().size())
                .sum();
        state.getStats().setTotalSources(state.getSources().size());
        state.getStats().setTotalPages(totalPages);
    }
}
