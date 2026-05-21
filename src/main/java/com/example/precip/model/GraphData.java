package com.example.precip.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphData {

    private int version = 1;
    private Map<String, GraphNode> nodes = new LinkedHashMap<>();
    private List<GraphEdge> edges = new ArrayList<>();

    // --- 嵌套类型 ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphNode {
        private String title;
        private String category;
        private String file;
        private String description;

        public GraphNode() {}

        public GraphNode(String title, String category, String file, String description) {
            this.title = title;
            this.category = category;
            this.file = file;
            this.description = description;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphEdge {
        private String source;
        private String target;
        private EdgeType type;

        public GraphEdge() {}

        public GraphEdge(String source, String target, EdgeType type) {
            this.source = source;
            this.target = target;
            this.type = type;
        }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getTarget() { return target; }
        public void setTarget(String target) { this.target = target; }
        public EdgeType getType() { return type; }
        public void setType(EdgeType type) { this.type = type; }
    }

    public enum EdgeType {
        LINK,
        IMPLEMENTS,
        DEPENDS_ON,
        RELATED_TO,
        CALLS
    }

    // --- getter/setter ---

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public Map<String, GraphNode> getNodes() { return nodes; }
    public void setNodes(Map<String, GraphNode> nodes) { this.nodes = nodes; }
    public List<GraphEdge> getEdges() { return edges; }
    public void setEdges(List<GraphEdge> edges) { this.edges = edges; }
}
