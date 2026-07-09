package io.github.springanalyzer.core.analyzer;

public record OrphanEndpoint(String serviceName, Endpoint endpoint) {
  public OrphanEndpoint {
    if (serviceName == null || serviceName.isBlank()) {
      throw new IllegalArgumentException("El nombre del servicio no puede estar vacio");
    }
    if (endpoint == null) {
      throw new IllegalArgumentException("El endpoint no puede ser nulo");
    }
  }
}
