package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyGraphTest {

  @Test
  void createsWithValidFields() {
    final ServiceNode node = new ServiceNode("order-service", ServiceVersionInfo.unknown());

    final DependencyGraph graph = new DependencyGraph(List.of(node), List.of(), List.of(), List.of());

    assertThat(graph.nodes()).containsExactly(node);
    assertThat(graph.edges()).isEmpty();
    assertThat(graph.orphanEndpoints()).isEmpty();
    assertThat(graph.orphanConsumptions()).isEmpty();
  }

  @Test
  void rejectsNullNodeList() {
    assertThatThrownBy(() -> new DependencyGraph(null, List.of(), List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullEdgeList() {
    assertThatThrownBy(() -> new DependencyGraph(List.of(), null, List.of(), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullOrphanEndpointList() {
    assertThatThrownBy(() -> new DependencyGraph(List.of(), List.of(), null, List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullOrphanConsumptionList() {
    assertThatThrownBy(() -> new DependencyGraph(List.of(), List.of(), List.of(), null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
