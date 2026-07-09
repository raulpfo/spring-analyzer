package io.github.springanalyzer.domain.entities;

import java.net.URI;
import java.util.Locale;
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
    final String host = hostOf(url);
    if (host == null) {
      return Optional.empty();
    }
    for (final ScmProvider provider : values()) {
      if (host.equals(provider.defaultHost) || host.endsWith("." + provider.defaultHost)) {
        return Optional.of(provider);
      }
    }
    return Optional.empty();
  }

  private static String hostOf(final String url) {
    if (url == null) {
      return null;
    }
    try {
      final String host = URI.create(url).getHost();
      return host == null ? null : host.toLowerCase(Locale.ROOT);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
