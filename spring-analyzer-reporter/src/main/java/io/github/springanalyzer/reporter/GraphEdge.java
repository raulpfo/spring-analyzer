package io.github.springanalyzer.reporter;

public record GraphEdge(String id, String from, String to, String label, String type, String title) {
  public GraphEdge {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("El id de la arista no puede estar vacio");
    }
    if (from == null || from.isBlank()) {
      throw new IllegalArgumentException("El origen de la arista no puede estar vacio");
    }
    if (to == null || to.isBlank()) {
      throw new IllegalArgumentException("El destino de la arista no puede estar vacio");
    }
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("El tipo de la arista no puede estar vacio");
    }
  }
}
