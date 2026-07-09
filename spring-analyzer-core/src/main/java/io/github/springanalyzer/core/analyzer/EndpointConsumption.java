package io.github.springanalyzer.core.analyzer;

public record EndpointConsumption(String targetService, String path, HttpMethod method) {
  public EndpointConsumption {
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("El path del consumo no puede estar vacio");
    }
    if (method == null) {
      throw new IllegalArgumentException("El metodo HTTP del consumo no puede ser nulo");
    }
    if (targetService != null && targetService.isBlank()) {
      throw new IllegalArgumentException("El servicio destino, si se especifica, no puede estar en blanco");
    }
  }
}
