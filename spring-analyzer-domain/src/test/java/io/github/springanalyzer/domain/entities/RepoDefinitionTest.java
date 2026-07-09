package io.github.springanalyzer.domain.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepoDefinitionTest {

  @Test
  void createsWithValidFields() {
    final RepoDefinition repo = new RepoDefinition("https://github.com/org/repo.git", "main", ScmProvider.GITHUB);

    assertThat(repo.url()).isEqualTo("https://github.com/org/repo.git");
    assertThat(repo.branch()).isEqualTo("main");
    assertThat(repo.provider()).isEqualTo(ScmProvider.GITHUB);
  }

  @Test
  void allowsNullBranch() {
    final RepoDefinition repo = new RepoDefinition("https://github.com/org/repo.git", null, ScmProvider.GITHUB);

    assertThat(repo.branch()).isNull();
  }

  @Test
  void rejectsBlankUrl() {
    assertThatThrownBy(() -> new RepoDefinition("  ", "main", ScmProvider.GITHUB))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullUrl() {
    assertThatThrownBy(() -> new RepoDefinition(null, "main", ScmProvider.GITHUB))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNullProvider() {
    assertThatThrownBy(() -> new RepoDefinition("https://github.com/org/repo.git", "main", null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void derivesRepoNameFromUrlStrippingGitSuffix() {
    final RepoDefinition repo = new RepoDefinition("https://github.com/org/user-service.git", "main", ScmProvider.GITHUB);

    assertThat(repo.repoName()).isEqualTo("user-service");
  }

  @Test
  void derivesRepoNameFromUrlWithoutGitSuffix() {
    final RepoDefinition repo = new RepoDefinition("https://gitlab.com/org/order-service", "main", ScmProvider.GITLAB);

    assertThat(repo.repoName()).isEqualTo("order-service");
  }

  @Test
  void derivesRepoNameIgnoringTrailingSlash() {
    final RepoDefinition repo = new RepoDefinition("https://github.com/org/user-service/", "main", ScmProvider.GITHUB);

    assertThat(repo.repoName()).isEqualTo("user-service");
  }
}
