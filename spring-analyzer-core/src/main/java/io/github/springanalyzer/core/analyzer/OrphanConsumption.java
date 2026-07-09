package io.github.springanalyzer.core.analyzer;

public record OrphanConsumption(String serviceName, EndpointConsumption consumption) {
  public OrphanConsumption {
    if (serviceName == null || serviceName.isBlank()) {
      throw new IllegalArgumentException("El nombre del servicio no puede estar vacio");
    }
    if (consumption == null) {
      throw new IllegalArgumentException("El consumo no puede ser nulo");
    }
  }
}
