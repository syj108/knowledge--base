package com.example.precip.link;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LinkInjectorTest {

    private final WikiLinkParser parser = new WikiLinkParser();

    @Test
    void shouldInjectLinkOnFirstOccurrence() {
        LinkInjector injector = new LinkInjector(null, parser);

        List<LinkInjector.TitleMapping> mappings = List.of(
                new LinkInjector.TitleMapping("REST API", "design/rest-api")
        );

        String content = "本系统提供 REST API 接口。\n在使用 REST API 时请注意认证。";
        String result = injector.injectLinks(content, "other/page", mappings);

        assertTrue(result.contains("[[design/rest-api|REST API]]"), "首次出现应被替换");
        // 第二行中的 REST API 不应被替换（已注入过该 slug）
        int linkCount = countOccurrences(result, "[[design/rest-api|REST API]]");
        assertEquals(1, linkCount, "只应注入一次");
    }

    @Test
    void shouldNotInjectSelfReference() {
        LinkInjector injector = new LinkInjector(null, parser);

        List<LinkInjector.TitleMapping> mappings = List.of(
                new LinkInjector.TitleMapping("REST API", "design/rest-api")
        );

        String content = "本文档介绍 REST API 设计。";
        String result = injector.injectLinks(content, "design/rest-api", mappings);

        assertFalse(result.contains("[["), "不应注入自引用");
    }

    @Test
    void shouldSkipHeadingLines() {
        LinkInjector injector = new LinkInjector(null, parser);

        List<LinkInjector.TitleMapping> mappings = List.of(
                new LinkInjector.TitleMapping("REST API", "design/rest-api")
        );

        String content = "# REST API 设计\n\n正文中提到 REST API 的用法。";
        String result = injector.injectLinks(content, "other/page", mappings);

        // 标题行不应被替换
        assertTrue(result.startsWith("# REST API 设计"), "标题不应被修改");
        // 正文应被替换
        assertTrue(result.contains("[[design/rest-api|REST API]]"));
    }

    @Test
    void shouldSkipCodeBlocks() {
        LinkInjector injector = new LinkInjector(null, parser);

        List<LinkInjector.TitleMapping> mappings = List.of(
                new LinkInjector.TitleMapping("REST API", "design/rest-api")
        );

        String content = "说明：\n```\nREST API 示例代码\n```\n正文中提到 REST API。";
        String result = injector.injectLinks(content, "other/page", mappings);

        // 代码块内不应被替换
        assertTrue(result.contains("REST API 示例代码"), "代码块内不应被修改");
    }

    @Test
    void shouldPreferLongerTitleMatch() {
        LinkInjector injector = new LinkInjector(null, parser);

        // 按标题长度降序排列（inject 方法内部会排序，此处模拟排序后结果）
        List<LinkInjector.TitleMapping> mappings = List.of(
                new LinkInjector.TitleMapping("REST API 设计", "design/rest-api-design"),
                new LinkInjector.TitleMapping("API", "design/api")
        );
        String content = "关于 REST API 设计的最佳实践。";
        String result = injector.injectLinks(content, "other/page", mappings);

        assertTrue(result.contains("[[design/rest-api-design|REST API 设计]]"),
                "应优先匹配更长的标题");
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
