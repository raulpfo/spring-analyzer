package io.github.springanalyzer.domain.entities;

public record RepoDefinition(String url, String branch, ScmProvider provider) {
  public RepoDefinition {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("La url del repositorio no puede estar vacia");
    }
    if (provider == null) {
      throw new IllegalArgumentException("El proveedor SCM del repositorio no puede ser nulo");
    }
  }
}
