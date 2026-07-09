package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrphanEndpointTest {

  private final Endpoint endpoint = new Endpoint(HttpMethod.GET, "/orders", "com.example.OrderController");

  @Test
  void createsWithValidFields() {
    final OrphanEndpoint orphan = new OrphanEndpoint("order-service", endpoint);

    assertThat(orphan.serviceName()).isEqualTo("order-service");
    assertThat(orphan.endpoint()).isEqualTo(endpoint);
  }

  @Test
  void rejectsBlankServiceName() {
    assertThatThrownBy(() -> new OrphanEndpoint("  ", endpoint))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullEndpoint() {
    assertThatThrownBy(() -> new OrphanEndpoint("order-service", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
