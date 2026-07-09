package io.github.springanalyzer.domain.entities;

import java.util.List;

public record RepoSourceConfig(List<RepoDefinition> repos) {
  public RepoSourceConfig {
    if (repos == null || repos.isEmpty()) {
      throw new IllegalArgumentException("La configuracion debe incluir al menos un repositorio");
    }
    repos = List.copyOf(repos);
  }
}
