package io.github.springanalyzer.core.analyzer;

public record ServiceNode(String serviceName, ServiceVersionInfo versionInfo) {
  public ServiceNode {
    if (serviceName == null || serviceName.isBlank()) {
      throw new IllegalArgumentException("El nombre del servicio no puede estar vacio");
    }
    if (versionInfo == null) {
      throw new IllegalArgumentException("La informacion de version no puede ser nula");
    }
  }
}
