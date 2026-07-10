package io.github.springanalyzer.reporter;

public record GraphNode(String id, String label, String group, String title) {
  public GraphNode {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("El id del nodo no puede estar vacio");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("La etiqueta del nodo no puede estar vacia");
    }
    if (group == null || group.isBlank()) {
      throw new IllegalArgumentException("El grupo del nodo no puede estar vacio");
    }
  }
}
