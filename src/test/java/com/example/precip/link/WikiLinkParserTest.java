package com.example.precip.link;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WikiLinkParserTest {

    private final WikiLinkParser parser = new WikiLinkParser();

    @Test
    void shouldParseSimpleLink() {
        List<WikiLinkParser.WikiLink> links = parser.parse("请参考 [[api-design/rest-api|REST API 设计]]");
        assertEquals(1, links.size());
        assertEquals("api-design/rest-api", links.get(0).slug());
        assertEquals("REST API 设计", links.get(0).displayName());
    }

    @Test
    void shouldParseLinkWithoutDisplayName() {
        List<WikiLinkParser.WikiLink> links = parser.parse("详见 [[api-design/rest-api]]");
        assertEquals(1, links.size());
        assertEquals("api-design/rest-api", links.get(0).slug());
        assertEquals("api-design/rest-api", links.get(0).displayName());
    }

    @Test
    void shouldParseMultipleLinks() {
        String text = "参考 [[a/b|标题A]] 和 [[c/d|标题B]]";
        List<WikiLinkParser.WikiLink> links = parser.parse(text);
        assertEquals(2, links.size());
        assertEquals("a/b", links.get(0).slug());
        assertEquals("c/d", links.get(1).slug());
    }

    @Test
    void shouldSkipLinksInsideCodeBlock() {
        String text = """
                正文中 [[valid/link|有效链接]]
                ```
                代码中 [[code/link|代码链接]]
                ```
                正文外 [[another/link|另一链接]]
                """;
        List<WikiLinkParser.WikiLink> links = parser.parse(text);
        assertEquals(2, links.size());
        assertEquals("valid/link", links.get(0).slug());
        assertEquals("another/link", links.get(1).slug());
    }

    @Test
    void shouldExtractSlugs() {
        String text = "[[a/b|A]] 和 [[c/d|C]] 再来 [[a/b|A重复]]";
        Set<String> slugs = parser.extractSlugs(text);
        assertEquals(Set.of("a/b", "c/d"), slugs);
    }

    @Test
    void shouldReturnEmptyForNullInput() {
        assertTrue(parser.parse(null).isEmpty());
        assertTrue(parser.parse("").isEmpty());
    }

    @Test
    void shouldHandleUnclosedCodeBlock() {
        String text = """
                正文 [[valid/link|有效]]
                ```
                代码 [[code/link|代码]]
                未闭合
                """;
        List<WikiLinkParser.WikiLink> links = parser.parse(text);
        assertEquals(1, links.size());
        assertEquals("valid/link", links.get(0).slug());
    }
}
