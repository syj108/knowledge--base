package com.example.precip.link;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析 [[slug|title]] 格式的 wiki 链接。
 */
@Component
public class WikiLinkParser {

    private static final Pattern LINK_PATTERN =
            Pattern.compile("\\[\\[([a-z0-9][a-z0-9\\-/]*)(?:\\|([^\\]]+))?]]");

    private static final Pattern FENCE_PATTERN = Pattern.compile("^```", Pattern.MULTILINE);

    public record WikiLink(String slug, String displayName, int start, int end) {}

    public List<WikiLink> parse(String markdown) {
        if (markdown == null || markdown.isEmpty()) return List.of();

        Set<int[]> codeFenceRanges = findCodeFenceRanges(markdown);

        List<WikiLink> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(markdown);
        while (matcher.find()) {
            if (isInsideCodeFence(matcher.start(), codeFenceRanges)) continue;

            String slug = matcher.group(1);
            String display = matcher.group(2) != null ? matcher.group(2) : slug;
            links.add(new WikiLink(slug, display, matcher.start(), matcher.end()));
        }
        return links;
    }

    public Set<String> extractSlugs(String markdown) {
        Set<String> slugs = new LinkedHashSet<>();
        for (WikiLink link : parse(markdown)) {
            slugs.add(link.slug());
        }
        return slugs;
    }

    private Set<int[]> findCodeFenceRanges(String text) {
        Set<int[]> ranges = new LinkedHashSet<>();
        Matcher m = FENCE_PATTERN.matcher(text);
        int openStart = -1;
        while (m.find()) {
            if (openStart < 0) {
                openStart = m.start();
            } else {
                ranges.add(new int[]{openStart, m.end()});
                openStart = -1;
            }
        }
        // 未闭合的代码块——从 openStart 到文本末尾都视为代码区域
        if (openStart >= 0) {
            ranges.add(new int[]{openStart, text.length()});
        }
        return ranges;
    }

    private boolean isInsideCodeFence(int position, Set<int[]> ranges) {
        for (int[] range : ranges) {
            if (position >= range[0] && position < range[1]) return true;
        }
        return false;
    }
}
