package io.github.springanalyzer.analyzers.spring;

public record Endpoint(HttpMethod method, String path, String owner) {
  public Endpoint {
    if (method == null) {
      throw new IllegalArgumentException("El metodo HTTP del endpoint no puede ser nulo");
    }
    if (path == null || path.isBlank()) {
      throw new IllegalArgumentException("El path del endpoint no puede estar vacio");
    }
    if (owner == null || owner.isBlank()) {
      throw new IllegalArgumentException("El propietario del endpoint no puede estar vacio");
    }
  }
}
