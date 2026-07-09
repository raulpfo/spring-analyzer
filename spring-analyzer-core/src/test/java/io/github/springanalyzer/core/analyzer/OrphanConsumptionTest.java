package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrphanConsumptionTest {

  private final EndpointConsumption consumption = new EndpointConsumption("order-service", "/orders", HttpMethod.GET);

  @Test
  void createsWithValidFields() {
    final OrphanConsumption orphan = new OrphanConsumption("user-service", consumption);

    assertThat(orphan.serviceName()).isEqualTo("user-service");
    assertThat(orphan.consumption()).isEqualTo(consumption);
  }

  @Test
  void rejectsBlankServiceName() {
    assertThatThrownBy(() -> new OrphanConsumption("  ", consumption))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullConsumption() {
    assertThatThrownBy(() -> new OrphanConsumption("user-service", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
