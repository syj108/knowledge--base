package com.example.precip.link;

import com.example.precip.kb.KnowledgeBaseService;
import com.example.precip.model.GraphData;
import com.example.precip.model.GraphData.EdgeType;
import com.example.precip.model.GraphData.GraphEdge;
import com.example.precip.model.GraphData.GraphNode;
import com.example.precip.model.KnowledgePage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * graph.json 图谱管理：更新节点、维护边。
 */
@Service
public class LinkGraph {

    private static final Logger log = LoggerFactory.getLogger(LinkGraph.class);

    private final KnowledgeBaseService kbService;
    private final WikiLinkParser wikiLinkParser;

    public LinkGraph(KnowledgeBaseService kbService, WikiLinkParser wikiLinkParser) {
        this.kbService = kbService;
        this.wikiLinkParser = wikiLinkParser;
    }

    public void updateGraph(List<KnowledgePage> pages) throws IOException {
        GraphData graph = kbService.readGraphData();

        for (KnowledgePage page : pages) {
            // 添加/更新节点
            GraphNode node = new GraphNode(
                    page.getTitle(),
                    page.getCategory(),
                    "pages/" + page.getSlug() + ".md",
                    null
            );
            graph.getNodes().put(page.getSlug(), node);

            // 解析页面内容中的 wiki 链接，构建 LINK 边
            if (page.getContent() != null) {
                // 先移除该页面的旧出边
                graph.getEdges().removeIf(e ->
                        e.getSource().equals(page.getSlug()) && e.getType() == EdgeType.LINK);

                Set<String> linkedSlugs = wikiLinkParser.extractSlugs(page.getContent());
                for (String target : linkedSlugs) {
                    if (!target.equals(page.getSlug())) {
                        graph.getEdges().add(new GraphEdge(page.getSlug(), target, EdgeType.LINK));
                    }
                }
            }
        }

        kbService.writeGraphData(graph);
        log.info("图谱已更新，共 {} 个节点、{} 条边", graph.getNodes().size(), graph.getEdges().size());
    }

    public boolean nodeExists(String slug) throws IOException {
        return kbService.readGraphData().getNodes().containsKey(slug);
    }
}
