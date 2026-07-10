package io.github.springanalyzer.reporter;

import io.github.springanalyzer.core.analyzer.Dependency;
import io.github.springanalyzer.core.analyzer.DependencyEdge;
import io.github.springanalyzer.core.analyzer.DependencyGraph;
import io.github.springanalyzer.core.analyzer.Endpoint;
import io.github.springanalyzer.core.analyzer.EndpointConsumption;
import io.github.springanalyzer.core.analyzer.HttpMethod;
import io.github.springanalyzer.core.analyzer.OrphanConsumption;
import io.github.springanalyzer.core.analyzer.OrphanEndpoint;
import io.github.springanalyzer.core.analyzer.ServiceNode;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlReportGeneratorTest {

  private final HtmlReportGenerator generator = new HtmlReportGenerator();

  @Test
  void rejectsANullGraph() {
    assertThatThrownBy(() -> generator.generate(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rendersARowPerNodeInTheVersionsTable() {
    final Document document = renderFixture();

    final Elements rows = document.select("#versions-table tbody tr");
    assertThat(rows).hasSize(2);

    final Element orderRow = rowStartingWith(rows, "order-service");
    assertThat(orderRow.select("td").get(1).text()).isEqualTo("3.4.0");
    assertThat(orderRow.select("td").get(2).text()).isEqualTo("21");
    assertThat(orderRow.select("td").get(3).text()).contains("org.apache.commons:commons-lang3:3.14.0");
  }

  @Test
  void rendersTheMermaidGraphDefinitionWithNodesAndTheMatchedEdge() {
    final Document document = renderFixture();

    final String mermaid = document.select("pre.mermaid").text();
    assertThat(mermaid).contains("graph LR");
    assertThat(mermaid).contains("order-service");
    assertThat(mermaid).contains("user-service");
    assertThat(mermaid).contains("GET /orders/{id}");
  }

  @Test
  void rendersOrphanEndpointsInTheAnomaliesSection() {
    final Document document = renderFixture();

    final Elements rows = document.select("#orphan-endpoints-table tbody tr");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).select("td").get(0).text()).isEqualTo("order-service");
    assertThat(rows.get(0).select("td").get(2).text()).isEqualTo("/orders/{id}/cancel");
  }

  @Test
  void rendersOrphanConsumptionsInTheAnomaliesSection() {
    final Document document = renderFixture();

    final Elements rows = document.select("#orphan-consumptions-table tbody tr");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).select("td").get(0).text()).isEqualTo("user-service");
    assertThat(rows.get(0).select("td").get(1).text()).isEqualTo("billing-service");
  }

  @Test
  void rendersOutdatedVersionsInTheAnomaliesSection() {
    final Document document = renderFixture();

    final Elements rows = document.select("#outdated-versions-table tbody tr");
    assertThat(rows).extracting(row -> row.select("td").get(0).text())
        .containsExactlyInAnyOrder("user-service", "user-service");
    assertThat(rows).extracting(row -> row.select("td").get(1).text())
        .containsExactlyInAnyOrder("Spring Boot", "org.apache.commons:commons-lang3");
  }

  @Test
  void rendersAFriendlyMessageInsteadOfATableWhenThereAreNoAnomalies() {
    final DependencyGraph graph = new DependencyGraph(
        List.of(new ServiceNode("order-service", new ServiceVersionInfo("3.4.0", "21", List.of()))), List.of(),
        List.of(), List.of());

    final Document document = Jsoup.parse(generator.generate(graph));

    assertThat(document.select("#orphan-endpoints-table")).isEmpty();
    assertThat(document.select("#orphan-consumptions-table")).isEmpty();
    assertThat(document.select("#outdated-versions-table")).isEmpty();
    assertThat(document.select(".anomaly-empty")).hasSize(3);
  }

  private Document renderFixture() {
    final ServiceNode orderService = new ServiceNode("order-service",
        new ServiceVersionInfo("3.4.0", "21", List.of(new Dependency("org.apache.commons", "commons-lang3", "3.14.0"))));
    final ServiceNode userService = new ServiceNode("user-service",
        new ServiceVersionInfo("2.7.5", "17", List.of(new Dependency("org.apache.commons", "commons-lang3", "3.9"))));

    final DependencyEdge edge = new DependencyEdge("user-service", "order-service",
        new EndpointConsumption("order-service", "/orders/42", HttpMethod.GET),
        new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController"));

    final OrphanEndpoint orphanEndpoint = new OrphanEndpoint("order-service",
        new Endpoint(HttpMethod.DELETE, "/orders/{id}/cancel", "com.example.OrderController"));

    final OrphanConsumption orphanConsumption = new OrphanConsumption("user-service",
        new EndpointConsumption("billing-service", "/invoices/{id}", HttpMethod.GET));

    final DependencyGraph graph = new DependencyGraph(List.of(orderService, userService), List.of(edge),
        List.of(orphanEndpoint), List.of(orphanConsumption));

    return Jsoup.parse(generator.generate(graph));
  }

  private static Element rowStartingWith(final Elements rows, final String serviceName) {
    return rows.stream().filter(row -> row.select("td").get(0).text().equals(serviceName)).findFirst()
        .orElseThrow(() -> new AssertionError("No row found for service " + serviceName));
  }
}
