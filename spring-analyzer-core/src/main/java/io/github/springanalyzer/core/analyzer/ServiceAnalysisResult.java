package io.github.springanalyzer.core.analyzer;

public record ServiceAnalysisResult(String repoName, String language) {
  public ServiceAnalysisResult {
    if (repoName == null || repoName.isBlank()) {
      throw new IllegalArgumentException("El nombre del repositorio no puede estar vacio");
    }
    if (language == null || language.isBlank()) {
      throw new IllegalArgumentException("El lenguaje no puede estar vacio");
    }
  }
}
