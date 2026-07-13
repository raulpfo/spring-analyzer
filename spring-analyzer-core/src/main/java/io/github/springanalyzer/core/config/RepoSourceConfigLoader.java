package io.github.springanalyzer.core.config;

import io.github.springanalyzer.core.analyzer.HttpMethod;
import io.github.springanalyzer.domain.entities.CustomAnnotationsConfig;
import io.github.springanalyzer.domain.entities.RepoDefinition;
import io.github.springanalyzer.domain.entities.RepoSourceConfig;
import io.github.springanalyzer.domain.entities.ScmProvider;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RepoSourceConfigLoader {

  public RepoSourceConfig load(final Path path) {
    try (Reader reader = Files.newBufferedReader(path)) {
      return parse(reader);
    } catch (IOException e) {
      throw new RepoSourceConfigException("No se pudo leer el fichero de configuracion: " + path, e);
    }
  }

  public RepoSourceConfig parse(final Reader reader) {
    final Object root = new Yaml().load(reader);
    if (!(root instanceof Map<?, ?> rootMap) || !(rootMap.get("repos") instanceof List<?> rawRepos)) {
      throw new RepoSourceConfigException("El fichero de configuracion debe tener una clave 'repos' con una lista de repositorios");
    }

    final List<RepoDefinition> repos = new ArrayList<>();
    int index = 0;
    for (final Object rawRepo : rawRepos) {
      repos.add(parseRepo(index, rawRepo));
      index++;
    }
    final CustomAnnotationsConfig customAnnotations = parseCustomAnnotations(rootMap.get("customAnnotations"));
    return new RepoSourceConfig(repos, customAnnotations);
  }

  private CustomAnnotationsConfig parseCustomAnnotations(final Object raw) {
    if (raw == null) {
      return CustomAnnotationsConfig.EMPTY;
    }
    if (!(raw instanceof Map<?, ?> rawMap)) {
      throw new RepoSourceConfigException("'customAnnotations' debe ser un objeto");
    }

    final List<String> controllers = parseAnnotationNameList(rawMap.get("controllers"), "customAnnotations.controllers");
    final List<String> consumers = parseAnnotationNameList(rawMap.get("consumers"), "customAnnotations.consumers");
    final Map<String, List<String>> mappings = parseMappings(rawMap.get("mappings"));
    return new CustomAnnotationsConfig(controllers, mappings, consumers);
  }

  private Map<String, List<String>> parseMappings(final Object raw) {
    if (raw == null) {
      return Map.of();
    }
    if (!(raw instanceof Map<?, ?> rawMap)) {
      throw new RepoSourceConfigException("'customAnnotations.mappings' debe ser un objeto con verbos HTTP como claves");
    }

    final Map<String, List<String>> mappings = new LinkedHashMap<>();
    for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
      final String verb = resolveHttpVerb(entry.getKey());
      mappings.put(verb, parseAnnotationNameList(entry.getValue(), "customAnnotations.mappings." + entry.getKey()));
    }
    return mappings;
  }

  private String resolveHttpVerb(final Object rawVerb) {
    if (!(rawVerb instanceof String verb) || verb.isBlank()) {
      throw new RepoSourceConfigException("Las claves de 'customAnnotations.mappings' deben ser texto");
    }
    try {
      return HttpMethod.valueOf(verb.trim().toUpperCase()).name();
    } catch (IllegalArgumentException e) {
      throw new RepoSourceConfigException(
          "Verbo HTTP desconocido '" + verb + "' en 'customAnnotations.mappings'. "
              + "Valores validos: GET, POST, PUT, DELETE, REQUEST");
    }
  }

  private List<String> parseAnnotationNameList(final Object raw, final String fieldPath) {
    if (raw == null) {
      return List.of();
    }
    if (!(raw instanceof List<?> rawList)) {
      throw new RepoSourceConfigException("'" + fieldPath + "' debe ser una lista de nombres de anotacion");
    }

    final List<String> names = new ArrayList<>();
    for (final Object rawName : rawList) {
      if (!(rawName instanceof String name) || name.isBlank()) {
        throw new RepoSourceConfigException("'" + fieldPath + "' contiene un nombre de anotacion invalido o vacio");
      }
      names.add(name.trim());
    }
    return names;
  }

  private RepoDefinition parseRepo(final int index, final Object rawRepo) {
    if (!(rawRepo instanceof Map<?, ?> repoMap)) {
      throw new RepoSourceConfigException("Entrada invalida en 'repos[" + index + "]': se esperaba un objeto");
    }

    if (!(repoMap.get("url") instanceof String url) || url.isBlank()) {
      throw new RepoSourceConfigException("Falta 'url' en 'repos[" + index + "]'");
    }

    final Object rawBranch = repoMap.get("branch");
    if (rawBranch != null && !(rawBranch instanceof String)) {
      throw new RepoSourceConfigException("El campo 'branch' en 'repos[" + index + "]' debe ser texto");
    }
    final String branch = (String) rawBranch;
    final ScmProvider provider = resolveProvider(index, repoMap.get("provider"), url);

    return new RepoDefinition(url, branch, provider);
  }

  private ScmProvider resolveProvider(final int index, final Object rawProvider, final String url) {
    if (rawProvider instanceof String providerName && !providerName.isBlank()) {
      try {
        return ScmProvider.valueOf(providerName.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new RepoSourceConfigException(
            "Proveedor SCM desconocido '" + providerName + "' en 'repos[" + index + "]'. Valores validos: github, gitlab");
      }
    }

    return ScmProvider.detectFromUrl(url)
        .orElseThrow(() -> new RepoSourceConfigException(
            "No se pudo determinar el proveedor SCM para '" + url + "' en 'repos[" + index + "]'. "
                + "Especifica 'provider: github' o 'provider: gitlab'"));
  }
}
