package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepoContextTest {

  @Test
  void createsWithValidFields(@TempDir final Path localPath) {
    final RepoContext context = new RepoContext("user-service", localPath);

    assertThat(context.repoName()).isEqualTo("user-service");
    assertThat(context.localPath()).isEqualTo(localPath);
  }

  @Test
  void rejectsBlankRepoName(@TempDir final Path localPath) {
    assertThatThrownBy(() -> new RepoContext("   ", localPath))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullRepoName(@TempDir final Path localPath) {
    assertThatThrownBy(() -> new RepoContext(null, localPath))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullLocalPath() {
    assertThatThrownBy(() -> new RepoContext("user-service", null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
