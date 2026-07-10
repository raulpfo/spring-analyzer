package io.github.springanalyzer.reporter;

import java.util.List;

public record GraphData(List<GraphNode> nodes, List<GraphEdge> edges) {
  public GraphData {
    if (nodes == null) {
      throw new IllegalArgumentException("La lista de nodos no puede ser nula");
    }
    if (edges == null) {
      throw new IllegalArgumentException("La lista de aristas no puede ser nula");
    }
    nodes = List.copyOf(nodes);
    edges = List.copyOf(edges);
  }
}
