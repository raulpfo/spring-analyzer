package io.github.springanalyzer.domain.entities;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAnnotationsConfigTest {

  @Test
  void treatsNullFieldsAsEmptyCollections() {
    final CustomAnnotationsConfig config = new CustomAnnotationsConfig(null, null, null);

    assertThat(config.controllers()).isEmpty();
    assertThat(config.mappings()).isEmpty();
    assertThat(config.consumers()).isEmpty();
  }

  @Test
  void isNotAffectedByMutationsOfTheInputCollections() {
    final List<String> controllers = new ArrayList<>(List.of("com.acme.fwk.MiController"));
    final Map<String, List<String>> mappings = new HashMap<>(Map.of("GET", new ArrayList<>(List.of("com.acme.fwk.MiGet"))));
    final List<String> consumers = new ArrayList<>(List.of("com.acme.fwk.MiFeignClient"));

    final CustomAnnotationsConfig config = new CustomAnnotationsConfig(controllers, mappings, consumers);
    controllers.clear();
    mappings.clear();
    consumers.clear();

    assertThat(config.controllers()).containsExactly("com.acme.fwk.MiController");
    assertThat(config.mappings()).containsEntry("GET", List.of("com.acme.fwk.MiGet"));
    assertThat(config.consumers()).containsExactly("com.acme.fwk.MiFeignClient");
  }

  @Test
  void emptyConstantHasNoAnnotationsConfigured() {
    assertThat(CustomAnnotationsConfig.EMPTY.controllers()).isEmpty();
    assertThat(CustomAnnotationsConfig.EMPTY.mappings()).isEmpty();
    assertThat(CustomAnnotationsConfig.EMPTY.consumers()).isEmpty();
  }
}
