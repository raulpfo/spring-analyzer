package io.github.springanalyzer.reporter;

import io.github.springanalyzer.core.analyzer.DependencyEdge;
import io.github.springanalyzer.core.analyzer.DependencyGraph;
import io.github.springanalyzer.core.analyzer.ServiceNode;

public class MermaidGraphRenderer {

  public String render(final DependencyGraph graph) {
    final StringBuilder mermaid = new StringBuilder("graph LR\n");
    for (final ServiceNode node : graph.nodes()) {
      mermaid.append("  ").append(nodeId(node.serviceName())).append("[\"").append(escape(node.serviceName()))
          .append("\"]\n");
    }
    for (final DependencyEdge edge : graph.edges()) {
      final String label = edge.endpoint().method() + " " + edge.endpoint().path();
      mermaid.append("  ").append(nodeId(edge.consumerService())).append(" -->|\"").append(escape(label))
          .append("\"| ").append(nodeId(edge.producerService())).append("\n");
    }
    return mermaid.toString();
  }

  private static String nodeId(final String serviceName) {
    return serviceName.replaceAll("[^a-zA-Z0-9_]", "_");
  }

  private static String escape(final String text) {
    // "report.html" renderiza este texto con th:text, que ya aplica el escapado
    // HTML estandar; aqui solo hace falta neutralizar la comilla para la propia
    // sintaxis de Mermaid, usando su secuencia de escape "#quot;" (no la entidad
    // HTML "&quot;", que quedaria doblemente escapada por Thymeleaf).
    return text.replace("\"", "#quot;");
  }
}
