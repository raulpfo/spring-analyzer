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

  public String repoName() {
    final String trimmed = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    final int lastSlash = trimmed.lastIndexOf('/');
    final String candidate = lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
    return candidate.endsWith(".git") ? candidate.substring(0, candidate.length() - 4) : candidate;
  }
}
