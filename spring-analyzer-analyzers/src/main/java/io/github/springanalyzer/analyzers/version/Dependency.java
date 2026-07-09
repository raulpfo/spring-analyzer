package io.github.springanalyzer.analyzers.version;

public record Dependency(String groupId, String artifactId, String version) {
  public Dependency {
    if (groupId == null || groupId.isBlank()) {
      throw new IllegalArgumentException("El groupId de la dependencia no puede estar vacio");
    }
    if (artifactId == null || artifactId.isBlank()) {
      throw new IllegalArgumentException("El artifactId de la dependencia no puede estar vacio");
    }
    if (version != null && version.isBlank()) {
      throw new IllegalArgumentException("La version de la dependencia, si se especifica, no puede estar en blanco");
    }
  }
}
