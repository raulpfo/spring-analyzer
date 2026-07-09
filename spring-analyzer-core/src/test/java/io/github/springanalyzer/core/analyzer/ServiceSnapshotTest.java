package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceSnapshotTest {

  @Test
  void createsWithValidFields() {
    final Endpoint endpoint = new Endpoint(HttpMethod.GET, "/orders", "com.example.OrderController");
    final EndpointConsumption consumption = new EndpointConsumption("order-service", "/orders", HttpMethod.GET);
    final ServiceVersionInfo versionInfo = ServiceVersionInfo.unknown();

    final ServiceSnapshot snapshot =
        new ServiceSnapshot("order-service", List.of(endpoint), List.of(consumption), versionInfo);

    assertThat(snapshot.serviceName()).isEqualTo("order-service");
    assertThat(snapshot.endpoints()).containsExactly(endpoint);
    assertThat(snapshot.consumptions()).containsExactly(consumption);
    assertThat(snapshot.versionInfo()).isEqualTo(versionInfo);
  }

  @Test
  void rejectsBlankServiceName() {
    assertThatThrownBy(() -> new ServiceSnapshot("  ", List.of(), List.of(), ServiceVersionInfo.unknown()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullEndpointList() {
    assertThatThrownBy(() -> new ServiceSnapshot("order-service", null, List.of(), ServiceVersionInfo.unknown()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullConsumptionList() {
    assertThatThrownBy(() -> new ServiceSnapshot("order-service", List.of(), null, ServiceVersionInfo.unknown()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullVersionInfo() {
    assertThatThrownBy(() -> new ServiceSnapshot("order-service", List.of(), List.of(), null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
