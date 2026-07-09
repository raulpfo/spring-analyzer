package io.github.springanalyzer.domain.entities;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepoSourceConfigTest {

  private static final RepoDefinition SAMPLE_REPO =
      new RepoDefinition("https://github.com/org/repo.git", "main", ScmProvider.GITHUB);

  @Test
  void createsWithNonEmptyRepoList() {
    final RepoSourceConfig config = new RepoSourceConfig(List.of(SAMPLE_REPO));

    assertThat(config.repos()).containsExactly(SAMPLE_REPO);
  }

  @Test
  void rejectsNullRepoList() {
    assertThatThrownBy(() -> new RepoSourceConfig(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsEmptyRepoList() {
    assertThatThrownBy(() -> new RepoSourceConfig(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void isNotAffectedByMutationsOfTheInputList() {
    final List<RepoDefinition> mutableRepos = new ArrayList<>(List.of(SAMPLE_REPO));
    final RepoSourceConfig config = new RepoSourceConfig(mutableRepos);

    mutableRepos.clear();

    assertThat(config.repos()).containsExactly(SAMPLE_REPO);
  }
}
