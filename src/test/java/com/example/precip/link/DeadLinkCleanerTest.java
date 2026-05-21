package com.example.precip.link;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeadLinkCleanerTest {

    private final WikiLinkParser parser = new WikiLinkParser();
    private final DeadLinkCleaner cleaner = new DeadLinkCleaner(null, parser);

    @Test
    void shouldKeepValidLinks() {
        Set<String> validSlugs = Set.of("design/rest-api", "guide/auth");
        String content = "请参考 [[design/rest-api|REST API]] 和 [[guide/auth|认证指南]]";
        String result = cleaner.cleanDeadLinks(content, validSlugs);
        assertEquals(content, result, "有效链接不应被修改");
    }

    @Test
    void shouldRemoveDeadLinksKeepingDisplayName() {
        Set<String> validSlugs = Set.of("design/rest-api");
        String content = "请参考 [[design/rest-api|REST API]] 和 [[guide/deleted|已删除的指南]]";
        String result = cleaner.cleanDeadLinks(content, validSlugs);

        assertTrue(result.contains("[[design/rest-api|REST API]]"), "有效链接应保留");
        assertFalse(result.contains("[[guide/deleted"), "死链应被移除");
        assertTrue(result.contains("已删除的指南"), "显示名应保留");
    }

    @Test
    void shouldHandleMultipleDeadLinks() {
        Set<String> validSlugs = Set.of();
        String content = "[[a/b|链接A]] 和 [[c/d|链接B]]";
        String result = cleaner.cleanDeadLinks(content, validSlugs);

        assertFalse(result.contains("[["), "所有死链都应被移除");
        assertTrue(result.contains("链接A"), "显示名A应保留");
        assertTrue(result.contains("链接B"), "显示名B应保留");
    }

    @Test
    void shouldHandleNoLinks() {
        Set<String> validSlugs = Set.of("a/b");
        String content = "这是一段没有链接的文本。";
        String result = cleaner.cleanDeadLinks(content, validSlugs);
        assertEquals(content, result);
    }
}
