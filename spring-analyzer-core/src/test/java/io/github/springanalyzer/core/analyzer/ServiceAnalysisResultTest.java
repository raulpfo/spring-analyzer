package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceAnalysisResultTest {

  @Test
  void createsWithValidFields() {
    final ServiceAnalysisResult result = new ServiceAnalysisResult("user-service", "java");

    assertThat(result.repoName()).isEqualTo("user-service");
    assertThat(result.language()).isEqualTo("java");
  }

  @Test
  void rejectsBlankRepoName() {
    assertThatThrownBy(() -> new ServiceAnalysisResult("  ", "java"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankLanguage() {
    assertThatThrownBy(() -> new ServiceAnalysisResult("user-service", "  "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
