package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceNodeTest {

  @Test
  void createsWithValidFields() {
    final ServiceVersionInfo versionInfo = ServiceVersionInfo.unknown();

    final ServiceNode node = new ServiceNode("order-service", versionInfo);

    assertThat(node.serviceName()).isEqualTo("order-service");
    assertThat(node.versionInfo()).isEqualTo(versionInfo);
  }

  @Test
  void rejectsBlankServiceName() {
    assertThatThrownBy(() -> new ServiceNode("  ", ServiceVersionInfo.unknown()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullVersionInfo() {
    assertThatThrownBy(() -> new ServiceNode("order-service", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
