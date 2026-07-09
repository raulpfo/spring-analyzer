package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndpointConsumptionTest {

  @Test
  void createsWithValidFields() {
    final EndpointConsumption consumption = new EndpointConsumption("order-service", "/orders", HttpMethod.GET);

    assertThat(consumption.targetService()).isEqualTo("order-service");
    assertThat(consumption.path()).isEqualTo("/orders");
    assertThat(consumption.method()).isEqualTo(HttpMethod.GET);
  }

  @Test
  void allowsNullTargetServiceToMarkItAsUnknown() {
    final EndpointConsumption consumption = new EndpointConsumption(null, "/orders", HttpMethod.GET);

    assertThat(consumption.targetService()).isNull();
  }

  @Test
  void rejectsBlankTargetService() {
    assertThatThrownBy(() -> new EndpointConsumption("  ", "/orders", HttpMethod.GET))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankPath() {
    assertThatThrownBy(() -> new EndpointConsumption("order-service", "  ", HttpMethod.GET))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullMethod() {
    assertThatThrownBy(() -> new EndpointConsumption("order-service", "/orders", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
