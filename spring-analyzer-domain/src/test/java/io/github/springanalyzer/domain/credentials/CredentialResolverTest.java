package io.github.springanalyzer.domain.credentials;

import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.entities.ScmProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialResolverTest {

  @Test
  void prefersExplicitCliFlagOverEverythingElse() {
    final CommandConfig config = commandConfig("cli-token", null, null);
    final CredentialResolver resolver = new CredentialResolver(config, Map.of(
        "CUSTOM_ENV", "custom-env-token",
        "GITHUB_TOKEN", "default-env-token"
    )::get);

    assertThat(resolver.resolve(ScmProvider.GITHUB)).contains("cli-token");
  }

  @Test
  void fallsBackToCustomTokenEnvWhenNoFlagGiven() {
    final CommandConfig config = commandConfig(null, null, "CUSTOM_ENV");
    final CredentialResolver resolver = new CredentialResolver(config, Map.of(
        "CUSTOM_ENV", "custom-env-token",
        "GITHUB_TOKEN", "default-env-token"
    )::get);

    assertThat(resolver.resolve(ScmProvider.GITHUB)).contains("custom-env-token");
  }

  @Test
  void fallsBackToProviderDefaultEnvVarWhenNoFlagNorCustomEnvGiven() {
    final CommandConfig config = commandConfig(null, null, null);
    final CredentialResolver resolver = new CredentialResolver(config, Map.of(
        "GITHUB_TOKEN", "default-env-token"
    )::get);

    assertThat(resolver.resolve(ScmProvider.GITHUB)).contains("default-env-token");
  }

  @Test
  void returnsEmptyWhenNoCredentialIsAvailable() {
    final CommandConfig config = commandConfig(null, null, null);
    final CredentialResolver resolver = new CredentialResolver(config, name -> null);

    assertThat(resolver.resolve(ScmProvider.GITHUB)).isEmpty();
  }

  @Test
  void resolveRequiredThrowsWithClearMessageWhenNothingIsAvailable() {
    final CommandConfig config = commandConfig(null, null, null);
    final CredentialResolver resolver = new CredentialResolver(config, name -> null);

    assertThatThrownBy(() -> resolver.resolveRequired(ScmProvider.GITHUB))
        .isInstanceOf(CredentialNotFoundException.class)
        .hasMessageContaining("--github-token")
        .hasMessageContaining("GITHUB_TOKEN");
  }

  @Test
  void resolvesGitlabTokenIndependentlyFromGithubToken() {
    final CommandConfig config = commandConfig(null, "gitlab-cli-token", null);
    final CredentialResolver resolver = new CredentialResolver(config, name -> null);

    assertThat(resolver.resolve(ScmProvider.GITLAB)).contains("gitlab-cli-token");
    assertThat(resolver.resolve(ScmProvider.GITHUB)).isEmpty();
  }

  private static CommandConfig commandConfig(final String githubToken, final String gitlabToken, final String tokenEnv) {
    return new CommandConfig("repos.yml", "report.html", ReportFormat.HTML, githubToken, gitlabToken, tokenEnv, 1, false, false);
  }
}
