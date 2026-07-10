package io.github.springanalyzer.reporter;

import io.github.springanalyzer.core.analyzer.DependencyEdge;
import io.github.springanalyzer.core.analyzer.DependencyGraph;
import io.github.springanalyzer.core.analyzer.Endpoint;
import io.github.springanalyzer.core.analyzer.EndpointConsumption;
import io.github.springanalyzer.core.analyzer.HttpMethod;
import io.github.springanalyzer.core.analyzer.ServiceNode;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MermaidGraphRendererTest {

  private final MermaidGraphRenderer renderer = new MermaidGraphRenderer();

  @Test
  void rendersOnlyTheHeaderForAnEmptyGraph() {
    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(), List.of(), List.of());

    assertThat(renderer.render(graph)).isEqualTo("graph LR\n");
  }

  @Test
  void rendersANodePerServiceWithASanitizedIdAndTheOriginalNameAsLabel() {
    final DependencyGraph graph = new DependencyGraph(
        List.of(new ServiceNode("order-service", ServiceVersionInfo.unknown())), List.of(), List.of(), List.of());

    assertThat(renderer.render(graph)).isEqualTo("graph LR\n  order_service[\"order-service\"]\n");
  }

  @Test
  void rendersAnEdgeLabeledWithTheProducerEndpointMethodAndPath() {
    final Endpoint endpoint = new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController");
    final EndpointConsumption consumption = new EndpointConsumption("order-service", "/orders/42", HttpMethod.GET);
    final DependencyEdge edge = new DependencyEdge("user-service", "order-service", consumption, endpoint);
    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(edge), List.of(), List.of());

    assertThat(renderer.render(graph)).isEqualTo("graph LR\n  user_service -->|\"GET /orders/{id}\"| order_service\n");
  }

  @Test
  void sanitizesNonAlphanumericCharactersInServiceNamesForMermaidNodeIds() {
    final DependencyGraph graph = new DependencyGraph(
        List.of(new ServiceNode("order.service-v2", ServiceVersionInfo.unknown())), List.of(), List.of(), List.of());

    assertThat(renderer.render(graph)).isEqualTo("graph LR\n  order_service_v2[\"order.service-v2\"]\n");
  }

  @Test
  void escapesDoubleQuotesInLabelsToAvoidBreakingMermaidSyntax() {
    final DependencyGraph graph = new DependencyGraph(
        List.of(new ServiceNode("order\"service", ServiceVersionInfo.unknown())), List.of(), List.of(), List.of());

    assertThat(renderer.render(graph)).isEqualTo("graph LR\n  order_service[\"order&quot;service\"]\n");
  }
}
