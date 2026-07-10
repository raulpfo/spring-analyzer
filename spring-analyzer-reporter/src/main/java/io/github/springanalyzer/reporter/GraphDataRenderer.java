package io.github.springanalyzer.reporter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.springanalyzer.core.analyzer.DependencyEdge;
import io.github.springanalyzer.core.analyzer.DependencyGraph;
import io.github.springanalyzer.core.analyzer.OrphanConsumption;
import io.github.springanalyzer.core.analyzer.OrphanEndpoint;
import io.github.springanalyzer.core.analyzer.ServiceNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GraphDataRenderer {

  static final String GROUP_NORMAL = "normal";
  static final String GROUP_ORPHAN_ENDPOINT = "orphan";
  static final String GROUP_OUTDATED_VERSION = "outdated";
  static final String GROUP_BOTH_ANOMALIES = "both";
  static final String GROUP_UNKNOWN_TARGET = "unknown";

  static final String EDGE_NORMAL = "normal";
  static final String EDGE_ORPHAN_CONSUMPTION = "orphan-consumption";

  private final ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

  public String render(final DependencyGraph graph, final List<OutdatedVersion> outdatedVersions) {
    final Map<String, List<OrphanEndpoint>> orphanEndpointsByService = graph.orphanEndpoints().stream()
        .collect(Collectors.groupingBy(OrphanEndpoint::serviceName));
    final Map<String, List<OutdatedVersion>> outdatedVersionsByService = outdatedVersions.stream()
        .collect(Collectors.groupingBy(OutdatedVersion::serviceName));

    final Map<String, GraphNode> nodesById = new LinkedHashMap<>();
    for (final ServiceNode node : graph.nodes()) {
      nodesById.put(node.serviceName(), serviceNode(node.serviceName(),
          orphanEndpointsByService.getOrDefault(node.serviceName(), List.of()),
          outdatedVersionsByService.getOrDefault(node.serviceName(), List.of())));
    }

    final List<GraphEdge> edges = new ArrayList<>();
    int edgeCounter = 0;
    for (final DependencyEdge edge : graph.edges()) {
      final String label = edge.endpoint().method() + " " + edge.endpoint().path();
      edges.add(new GraphEdge("e" + edgeCounter++, edge.consumerService(), edge.producerService(), label,
          EDGE_NORMAL, null));
    }
    for (final OrphanConsumption orphan : graph.orphanConsumptions()) {
      final String targetLabel = orphan.consumption().targetService() != null ? orphan.consumption().targetService()
          : "destino desconocido";
      final String targetId = "unknown:" + targetLabel;
      nodesById.putIfAbsent(targetId,
          new GraphNode(targetId, targetLabel, GROUP_UNKNOWN_TARGET, "Servicio no encontrado en el analisis"));
      final String label = orphan.consumption().method() + " " + orphan.consumption().path();
      edges.add(new GraphEdge("e" + edgeCounter++, orphan.serviceName(), targetId, label, EDGE_ORPHAN_CONSUMPTION,
          "Consumidor sin destino resuelto"));
    }

    try {
      return objectMapper.writeValueAsString(new GraphData(List.copyOf(nodesById.values()), edges));
    } catch (final JsonProcessingException e) {
      throw new IllegalStateException("No se pudo serializar el grafo de dependencias", e);
    }
  }

  private static GraphNode serviceNode(final String serviceName, final List<OrphanEndpoint> orphanEndpoints,
      final List<OutdatedVersion> outdatedVersions) {
    final boolean hasOrphanEndpoints = !orphanEndpoints.isEmpty();
    final boolean hasOutdatedVersions = !outdatedVersions.isEmpty();
    final String group;
    if (hasOrphanEndpoints && hasOutdatedVersions) {
      group = GROUP_BOTH_ANOMALIES;
    } else if (hasOrphanEndpoints) {
      group = GROUP_ORPHAN_ENDPOINT;
    } else if (hasOutdatedVersions) {
      group = GROUP_OUTDATED_VERSION;
    } else {
      group = GROUP_NORMAL;
    }
    return new GraphNode(serviceName, serviceName, group, titleFor(orphanEndpoints, outdatedVersions));
  }

  private static String titleFor(final List<OrphanEndpoint> orphanEndpoints,
      final List<OutdatedVersion> outdatedVersions) {
    final List<String> reasons = new ArrayList<>();
    if (!orphanEndpoints.isEmpty()) {
      reasons.add(orphanEndpoints.size() + " endpoint(s) sin consumidores");
    }
    if (!outdatedVersions.isEmpty()) {
      reasons.add(outdatedVersions.size() + " dependencia(s) desactualizada(s)");
    }
    return reasons.isEmpty() ? null : String.join(", ", reasons);
  }
}
