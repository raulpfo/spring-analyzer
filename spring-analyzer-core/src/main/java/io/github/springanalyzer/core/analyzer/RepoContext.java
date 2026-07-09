package io.github.springanalyzer.core.analyzer;

import java.nio.file.Path;

public record RepoContext(String repoName, Path localPath) {
  public RepoContext {
    if (repoName == null || repoName.isBlank()) {
      throw new IllegalArgumentException("El nombre del repositorio no puede estar vacio");
    }
    if (localPath == null) {
      throw new IllegalArgumentException("La ruta local del repositorio no puede ser nula");
    }
  }
}
