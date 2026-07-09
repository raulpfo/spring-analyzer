package io.github.springanalyzer.core.analyzer;

import java.util.List;

public record DependencyGraph(List<ServiceNode> nodes, List<DependencyEdge> edges,
    List<OrphanEndpoint> orphanEndpoints, List<OrphanConsumption> orphanConsumptions) {
  public DependencyGraph {
    if (nodes == null) {
      throw new IllegalArgumentException("La lista de nodos no puede ser nula");
    }
    if (edges == null) {
      throw new IllegalArgumentException("La lista de aristas no puede ser nula");
    }
    if (orphanEndpoints == null) {
      throw new IllegalArgumentException("La lista de endpoints huerfanos no puede ser nula");
    }
    if (orphanConsumptions == null) {
      throw new IllegalArgumentException("La lista de consumos huerfanos no puede ser nula");
    }
    nodes = List.copyOf(nodes);
    edges = List.copyOf(edges);
    orphanEndpoints = List.copyOf(orphanEndpoints);
    orphanConsumptions = List.copyOf(orphanConsumptions);
  }
}
