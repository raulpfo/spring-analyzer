package io.github.springanalyzer.core.analyzer;

import java.util.List;

public record ServiceSnapshot(String serviceName, List<Endpoint> endpoints, List<EndpointConsumption> consumptions,
    ServiceVersionInfo versionInfo) {
  public ServiceSnapshot {
    if (serviceName == null || serviceName.isBlank()) {
      throw new IllegalArgumentException("El nombre del servicio no puede estar vacio");
    }
    if (endpoints == null) {
      throw new IllegalArgumentException("La lista de endpoints no puede ser nula");
    }
    if (consumptions == null) {
      throw new IllegalArgumentException("La lista de consumos no puede ser nula");
    }
    if (versionInfo == null) {
      throw new IllegalArgumentException("La informacion de version no puede ser nula");
    }
    endpoints = List.copyOf(endpoints);
    consumptions = List.copyOf(consumptions);
  }
}
