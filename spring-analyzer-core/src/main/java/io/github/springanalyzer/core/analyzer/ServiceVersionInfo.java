package io.github.springanalyzer.core.analyzer;

import java.util.List;

public record ServiceVersionInfo(String springBootVersion, String javaVersion, List<Dependency> dependencies) {
  public ServiceVersionInfo {
    if (springBootVersion != null && springBootVersion.isBlank()) {
      throw new IllegalArgumentException("La version de Spring Boot, si se especifica, no puede estar en blanco");
    }
    if (javaVersion != null && javaVersion.isBlank()) {
      throw new IllegalArgumentException("La version de Java, si se especifica, no puede estar en blanco");
    }
    if (dependencies == null) {
      throw new IllegalArgumentException("La lista de dependencias no puede ser nula");
    }
    dependencies = List.copyOf(dependencies);
  }

  public static ServiceVersionInfo unknown() {
    return new ServiceVersionInfo(null, null, List.of());
  }
}
