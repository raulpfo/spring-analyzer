package io.github.springanalyzer.domain.entities;

import java.util.Optional;

public enum ScmProvider {
  GITHUB("GITHUB_TOKEN", "github.com"),
  GITLAB("GITLAB_TOKEN", "gitlab.com");

  private final String defaultTokenEnvVar;
  private final String defaultHost;

  ScmProvider(final String defaultTokenEnvVar, final String defaultHost) {
    this.defaultTokenEnvVar = defaultTokenEnvVar;
    this.defaultHost = defaultHost;
  }

  public String defaultTokenEnvVar() {
    return defaultTokenEnvVar;
  }

  public static Optional<ScmProvider> detectFromUrl(final String url) {
    if (url == null) {
      return Optional.empty();
    }
    for (final ScmProvider provider : values()) {
      if (url.contains(provider.defaultHost)) {
        return Optional.of(provider);
      }
    }
    return Optional.empty();
  }
}
