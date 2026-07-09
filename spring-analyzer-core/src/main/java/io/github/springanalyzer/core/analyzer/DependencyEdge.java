package io.github.springanalyzer.core.analyzer;

public record DependencyEdge(String consumerService, String producerService, EndpointConsumption consumption,
    Endpoint endpoint) {
  public DependencyEdge {
    if (consumerService == null || consumerService.isBlank()) {
      throw new IllegalArgumentException("El servicio consumidor no puede estar vacio");
    }
    if (producerService == null || producerService.isBlank()) {
      throw new IllegalArgumentException("El servicio productor no puede estar vacio");
    }
    if (consumption == null) {
      throw new IllegalArgumentException("El consumo no puede ser nulo");
    }
    if (endpoint == null) {
      throw new IllegalArgumentException("El endpoint no puede ser nulo");
    }
  }
}
