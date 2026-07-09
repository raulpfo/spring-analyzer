package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceVersionInfoTest {

  @Test
  void createsWithValidFields() {
    final Dependency dependency = new Dependency("org.springframework.boot", "spring-boot-starter-web", "3.4.0");

    final ServiceVersionInfo info = new ServiceVersionInfo("3.4.0", "21", List.of(dependency));

    assertThat(info.springBootVersion()).isEqualTo("3.4.0");
    assertThat(info.javaVersion()).isEqualTo("21");
    assertThat(info.dependencies()).containsExactly(dependency);
  }

  @Test
  void allowsNullSpringBootAndJavaVersionsToMarkThemAsUnknown() {
    final ServiceVersionInfo info = new ServiceVersionInfo(null, null, List.of());

    assertThat(info.springBootVersion()).isNull();
    assertThat(info.javaVersion()).isNull();
  }

  @Test
  void rejectsBlankSpringBootVersion() {
    assertThatThrownBy(() -> new ServiceVersionInfo("  ", "21", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankJavaVersion() {
    assertThatThrownBy(() -> new ServiceVersionInfo("3.4.0", "  ", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullDependencyList() {
    assertThatThrownBy(() -> new ServiceVersionInfo("3.4.0", "21", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unknownFactoryReturnsAllUnknownFields() {
    final ServiceVersionInfo info = ServiceVersionInfo.unknown();

    assertThat(info.springBootVersion()).isNull();
    assertThat(info.javaVersion()).isNull();
    assertThat(info.dependencies()).isEmpty();
  }
}
