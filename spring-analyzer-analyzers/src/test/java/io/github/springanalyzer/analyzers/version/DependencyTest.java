package io.github.springanalyzer.analyzers.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyTest {

  @Test
  void createsWithValidFields() {
    final Dependency dependency = new Dependency("org.springframework.boot", "spring-boot-starter-web", "3.4.0");

    assertThat(dependency.groupId()).isEqualTo("org.springframework.boot");
    assertThat(dependency.artifactId()).isEqualTo("spring-boot-starter-web");
    assertThat(dependency.version()).isEqualTo("3.4.0");
  }

  @Test
  void allowsNullVersionToMarkItAsUnknown() {
    final Dependency dependency = new Dependency("org.springframework.boot", "spring-boot-starter-web", null);

    assertThat(dependency.version()).isNull();
  }

  @Test
  void rejectsBlankGroupId() {
    assertThatThrownBy(() -> new Dependency("  ", "spring-boot-starter-web", "3.4.0"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankArtifactId() {
    assertThatThrownBy(() -> new Dependency("org.springframework.boot", "  ", "3.4.0"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankVersion() {
    assertThatThrownBy(() -> new Dependency("org.springframework.boot", "spring-boot-starter-web", "  "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
