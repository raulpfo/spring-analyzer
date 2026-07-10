package io.github.springanalyzer.reporter;

public record OutdatedVersion(String serviceName, String label, String currentVersion, String latestVersion) {
  public OutdatedVersion {
    if (serviceName == null || serviceName.isBlank()) {
      throw new IllegalArgumentException("El nombre del servicio no puede estar vacio");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("La etiqueta de la dependencia no puede estar vacia");
    }
    if (currentVersion == null || currentVersion.isBlank()) {
      throw new IllegalArgumentException("La version actual no puede estar vacia");
    }
    if (latestVersion == null || latestVersion.isBlank()) {
      throw new IllegalArgumentException("La ultima version no puede estar vacia");
    }
  }
}
