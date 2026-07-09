package io.github.springanalyzer.core.config;

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
    return new RepoSourceConfig(repos);
  }

  private RepoDefinition parseRepo(final int index, final Object rawRepo) {
    if (!(rawRepo instanceof Map<?, ?> repoMap)) {
      throw new RepoSourceConfigException("Entrada invalida en 'repos[" + index + "]': se esperaba un objeto");
    }

    if (!(repoMap.get("url") instanceof String url) || url.isBlank()) {
      throw new RepoSourceConfigException("Falta 'url' en 'repos[" + index + "]'");
    }

    final String branch = repoMap.get("branch") instanceof String branchValue ? branchValue : null;
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
