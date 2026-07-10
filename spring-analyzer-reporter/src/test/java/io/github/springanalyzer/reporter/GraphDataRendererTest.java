package io.github.springanalyzer.reporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.springanalyzer.core.analyzer.DependencyEdge;
import io.github.springanalyzer.core.analyzer.DependencyGraph;
import io.github.springanalyzer.core.analyzer.Endpoint;
import io.github.springanalyzer.core.analyzer.EndpointConsumption;
import io.github.springanalyzer.core.analyzer.HttpMethod;
import io.github.springanalyzer.core.analyzer.OrphanConsumption;
import io.github.springanalyzer.core.analyzer.OrphanEndpoint;
import io.github.springanalyzer.core.analyzer.ServiceNode;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GraphDataRendererTest {

  private final GraphDataRenderer renderer = new GraphDataRenderer();
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void rendersEmptyNodesAndEdgesForAnEmptyGraph() throws Exception {
    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(), List.of(), List.of());

    final JsonNode json = parse(renderer.render(graph, List.of()));

    assertThat(json.get("nodes")).isEmpty();
    assertThat(json.get("edges")).isEmpty();
  }

  @Test
  void rendersANodePerServiceInTheNormalGroupWhenThereAreNoAnomalies() throws Exception {
    final DependencyGraph graph = new DependencyGraph(
        List.of(new ServiceNode("order-service", ServiceVersionInfo.unknown())), List.of(), List.of(), List.of());

    final JsonNode node = singleNode(renderer.render(graph, List.of()));

    assertThat(node.get("id").asText()).isEqualTo("order-service");
    assertThat(node.get("label").asText()).isEqualTo("order-service");
    assertThat(node.get("group").asText()).isEqualTo("normal");
  }

  @Test
  void rendersAnEdgeLabeledWithTheProducerEndpointMethodAndPath() throws Exception {
    final Endpoint endpoint = new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController");
    final EndpointConsumption consumption = new EndpointConsumption("order-service", "/orders/42", HttpMethod.GET);
    final DependencyEdge edge = new DependencyEdge("user-service", "order-service", consumption, endpoint);
    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(edge), List.of(), List.of());

    final JsonNode json = parse(renderer.render(graph, List.of()));
    final JsonNode renderedEdge = json.get("edges").get(0);

    assertThat(renderedEdge.get("from").asText()).isEqualTo("user-service");
    assertThat(renderedEdge.get("to").asText()).isEqualTo("order-service");
    assertThat(renderedEdge.get("label").asText()).isEqualTo("GET /orders/{id}");
    assertThat(renderedEdge.get("type").asText()).isEqualTo("normal");
  }

  @Test
  void marksAServiceWithAnOrphanEndpointInTheOrphanGroup() throws Exception {
    final DependencyGraph graph = new DependencyGraph(
        List.of(new ServiceNode("order-service", ServiceVersionInfo.unknown())), List.of(),
        List.of(new OrphanEndpoint("order-service",
            new Endpoint(HttpMethod.DELETE, "/orders/{id}/cancel", "com.example.OrderController"))),
        List.of());

    final JsonNode node = singleNode(renderer.render(graph, List.of()));

    assertThat(node.get("group").asText()).isEqualTo("orphan");
  }

  @Test
  void marksAServiceWithAnOutdatedVersionInTheOutdatedGroup() throws Exception {
    final DependencyGraph graph = new DependencyGraph(
        List.of(new ServiceNode("order-service", ServiceVersionInfo.unknown())), List.of(), List.of(), List.of());
    final List<OutdatedVersion> outdatedVersions = List
        .of(new OutdatedVersion("order-service", "Spring Boot", "2.7.5", "3.4.0"));

    final JsonNode node = singleNode(renderer.render(graph, outdatedVersions));

    assertThat(node.get("group").asText()).isEqualTo("outdated");
  }

  @Test
  void marksAServiceWithBothAnomaliesInTheBothGroup() throws Exception {
    final DependencyGraph graph = new DependencyGraph(
        List.of(new ServiceNode("order-service", ServiceVersionInfo.unknown())), List.of(),
        List.of(new OrphanEndpoint("order-service",
            new Endpoint(HttpMethod.DELETE, "/orders/{id}/cancel", "com.example.OrderController"))),
        List.of());
    final List<OutdatedVersion> outdatedVersions = List
        .of(new OutdatedVersion("order-service", "Spring Boot", "2.7.5", "3.4.0"));

    final JsonNode node = singleNode(renderer.render(graph, outdatedVersions));

    assertThat(node.get("group").asText()).isEqualTo("both");
  }

  @Test
  void rendersAnOrphanConsumptionAsAnEdgeToASyntheticUnknownNode() throws Exception {
    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(), List.of(),
        List.of(new OrphanConsumption("user-service",
            new EndpointConsumption("billing-service", "/invoices/{id}", HttpMethod.GET))));

    final JsonNode json = parse(renderer.render(graph, List.of()));
    final JsonNode unknownNode = json.get("nodes").get(0);
    final JsonNode unknownEdge = json.get("edges").get(0);

    assertThat(unknownNode.get("group").asText()).isEqualTo("unknown");
    assertThat(unknownNode.get("label").asText()).isEqualTo("billing-service");
    assertThat(unknownEdge.get("from").asText()).isEqualTo("user-service");
    assertThat(unknownEdge.get("to").asText()).isEqualTo(unknownNode.get("id").asText());
    assertThat(unknownEdge.get("type").asText()).isEqualTo("orphan-consumption");
    assertThat(unknownEdge.get("label").asText()).isEqualTo("GET /invoices/{id}");
  }

  @Test
  void groupsOrphanConsumptionsWithoutAKnownTargetUnderASharedUnknownNode() throws Exception {
    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(), List.of(),
        List.of(new OrphanConsumption("user-service", new EndpointConsumption(null, "/a", HttpMethod.GET)),
            new OrphanConsumption("billing-service", new EndpointConsumption(null, "/b", HttpMethod.POST))));

    final JsonNode json = parse(renderer.render(graph, List.of()));

    assertThat(json.get("nodes")).hasSize(1);
    assertThat(json.get("nodes").get(0).get("label").asText()).isEqualTo("destino desconocido");
    assertThat(json.get("edges")).hasSize(2);
  }

  private JsonNode singleNode(final String json) throws Exception {
    final JsonNode nodes = parse(json).get("nodes");
    assertThat(nodes).hasSize(1);
    return nodes.get(0);
  }

  private JsonNode parse(final String json) throws Exception {
    return objectMapper.readTree(json);
  }
}
