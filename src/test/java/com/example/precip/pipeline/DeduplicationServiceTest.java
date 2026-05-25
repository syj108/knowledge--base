package com.example.precip.pipeline;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 DeduplicationService 中的标题相似度计算逻辑。
 */
class DeduplicationServiceTest {

    @Test
    void titleSimilarity_identicalTitles() {
        double score = DeduplicationService.titleSimilarity("IDaaS CIAM产品简介", "IDaaS CIAM产品简介");
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void titleSimilarity_similarChineseTitles() {
        double score = DeduplicationService.titleSimilarity(
                "IDaaS CIAM产品简介", "IDaaS CIAM产品白皮书");
        assertTrue(score > 0.2, "有重叠词汇的标题应有较高相似度，实际: " + score);

        double highOverlap = DeduplicationService.titleSimilarity(
                "IDaaS CIAM 产品简介文档", "IDaaS CIAM 产品简介概述");
        assertTrue(highOverlap > score, "更相似的标题应有更高分数");
    }

    @Test
    void titleSimilarity_differentTitles() {
        double score = DeduplicationService.titleSimilarity("ECS弹性云服务器", "SRS流媒体服务");
        assertTrue(score < 0.35, "完全不同的标题相似度应低于阈值，实际: " + score);
    }

    @Test
    void titleSimilarity_nullHandling() {
        assertEquals(0, DeduplicationService.titleSimilarity(null, "标题"));
        assertEquals(0, DeduplicationService.titleSimilarity("标题", null));
        assertEquals(0, DeduplicationService.titleSimilarity(null, null));
    }

    @Test
    void titleSimilarity_emptyStrings() {
        double score = DeduplicationService.titleSimilarity("", "");
        assertEquals(1.0, score, 0.001, "两个空字符串应视为完全相同");
    }

    @Test
    void titleSimilarity_partialOverlap() {
        double score = DeduplicationService.titleSimilarity(
                "应用身份服务 IDaaS", "IDaaS 身份即服务");
        assertTrue(score > 0, "有部分重叠的标题应有正相似度");
    }
}
