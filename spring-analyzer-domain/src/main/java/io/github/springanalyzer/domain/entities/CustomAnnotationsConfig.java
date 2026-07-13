package io.github.springanalyzer.domain.entities;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record CustomAnnotationsConfig(List<String> controllers, Map<String, List<String>> mappings,
    List<String> consumers) {

  public static final CustomAnnotationsConfig EMPTY = new CustomAnnotationsConfig(List.of(), Map.of(), List.of());

  public CustomAnnotationsConfig {
    controllers = controllers == null ? List.of() : List.copyOf(controllers);
    mappings = mappings == null ? Map.of() : mappings.entrySet().stream()
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    consumers = consumers == null ? List.of() : List.copyOf(consumers);
  }
}
