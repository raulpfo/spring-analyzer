package io.github.springanalyzer.domain.entities;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScmProviderTest {

  @Test
  void detectsGithubFromUrl() {
    assertThat(ScmProvider.detectFromUrl("https://github.com/org/repo.git")).contains(ScmProvider.GITHUB);
  }

  @Test
  void detectsGitlabFromUrl() {
    assertThat(ScmProvider.detectFromUrl("https://gitlab.com/org/repo.git")).contains(ScmProvider.GITLAB);
  }

  @Test
  void returnsEmptyForUnknownHost() {
    assertThat(ScmProvider.detectFromUrl("https://bitbucket.org/org/repo.git")).isEmpty();
  }

  @Test
  void doesNotMatchHostsThatMerelyContainTheDefaultHostAsASubstring() {
    assertThat(ScmProvider.detectFromUrl("https://git.mycompany-github.com/org/repo.git")).isEmpty();
  }

  @Test
  void detectsGithubOnEnterpriseSubdomain() {
    assertThat(ScmProvider.detectFromUrl("https://ghe.github.com/org/repo.git")).contains(ScmProvider.GITHUB);
  }

  @Test
  void returnsEmptyForNullUrl() {
    assertThat(ScmProvider.detectFromUrl(null)).isEmpty();
  }

  @Test
  void exposesDefaultTokenEnvVarPerProvider() {
    assertThat(ScmProvider.GITHUB.defaultTokenEnvVar()).isEqualTo("GITHUB_TOKEN");
    assertThat(ScmProvider.GITLAB.defaultTokenEnvVar()).isEqualTo("GITLAB_TOKEN");
  }
}
