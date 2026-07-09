package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyEdgeTest {

  private final EndpointConsumption consumption = new EndpointConsumption("order-service", "/orders", HttpMethod.GET);
  private final Endpoint endpoint = new Endpoint(HttpMethod.GET, "/orders", "com.example.OrderController");

  @Test
  void createsWithValidFields() {
    final DependencyEdge edge = new DependencyEdge("user-service", "order-service", consumption, endpoint);

    assertThat(edge.consumerService()).isEqualTo("user-service");
    assertThat(edge.producerService()).isEqualTo("order-service");
    assertThat(edge.consumption()).isEqualTo(consumption);
    assertThat(edge.endpoint()).isEqualTo(endpoint);
  }

  @Test
  void rejectsBlankConsumerService() {
    assertThatThrownBy(() -> new DependencyEdge("  ", "order-service", consumption, endpoint))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankProducerService() {
    assertThatThrownBy(() -> new DependencyEdge("user-service", "  ", consumption, endpoint))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullConsumption() {
    assertThatThrownBy(() -> new DependencyEdge("user-service", "order-service", null, endpoint))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullEndpoint() {
    assertThatThrownBy(() -> new DependencyEdge("user-service", "order-service", consumption, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
