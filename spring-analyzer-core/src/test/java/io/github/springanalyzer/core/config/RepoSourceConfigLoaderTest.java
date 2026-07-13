package io.github.springanalyzer.core.config;

import io.github.springanalyzer.domain.entities.CustomAnnotationsConfig;
import io.github.springanalyzer.domain.entities.RepoDefinition;
import io.github.springanalyzer.domain.entities.RepoSourceConfig;
import io.github.springanalyzer.domain.entities.ScmProvider;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepoSourceConfigLoaderTest {

  private final RepoSourceConfigLoader loader = new RepoSourceConfigLoader();

  @Test
  void parsesRepoListWithAutoDetectedAndExplicitProviders() {
    final String yaml = """
        repos:
          - url: https://github.com/org/user-service.git
            branch: main
          - url: https://gitlab.com/org/order-service.git
          - url: https://scm.internal.example/org/auth-service.git
            provider: github
        """;

    final RepoSourceConfig config = loader.parse(new StringReader(yaml));

    assertThat(config.repos()).containsExactly(
        new RepoDefinition("https://github.com/org/user-service.git", "main", ScmProvider.GITHUB),
        new RepoDefinition("https://gitlab.com/org/order-service.git", null, ScmProvider.GITLAB),
        new RepoDefinition("https://scm.internal.example/org/auth-service.git", null, ScmProvider.GITHUB)
    );
    assertThat(config.customAnnotations()).isEqualTo(CustomAnnotationsConfig.EMPTY);
  }

  @Test
  void parsesCustomAnnotationsBlockWhenPresent() {
    final String yaml = """
        customAnnotations:
          controllers: [com.acme.fwk.MiController]
          mappings:
            GET: [com.acme.fwk.MiGet]
            post: [com.acme.fwk.MiPost]
          consumers: [com.acme.fwk.MiFeignClient]
        repos:
          - url: https://github.com/org/user-service.git
        """;

    final RepoSourceConfig config = loader.parse(new StringReader(yaml));

    assertThat(config.customAnnotations()).isEqualTo(new CustomAnnotationsConfig(
        List.of("com.acme.fwk.MiController"),
        Map.of("GET", List.of("com.acme.fwk.MiGet"), "POST", List.of("com.acme.fwk.MiPost")),
        List.of("com.acme.fwk.MiFeignClient")));
  }

  @Test
  void failsWhenCustomAnnotationsControllersEntryIsBlank() {
    final String yaml = """
        customAnnotations:
          controllers: [""]
        repos:
          - url: https://github.com/org/user-service.git
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("customAnnotations.controllers");
  }

  @Test
  void failsWhenCustomAnnotationsMappingsVerbIsUnknown() {
    final String yaml = """
        customAnnotations:
          mappings:
            PATCH: [com.acme.fwk.MiPatch]
        repos:
          - url: https://github.com/org/user-service.git
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("PATCH");
  }

  @Test
  void failsWhenCustomAnnotationsIsNotAnObject() {
    final String yaml = """
        customAnnotations: "com.acme.fwk.MiController"
        repos:
          - url: https://github.com/org/user-service.git
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("customAnnotations");
  }

  @Test
  void failsWhenCustomAnnotationsControllersIsNotAList() {
    final String yaml = """
        customAnnotations:
          controllers: com.acme.fwk.MiController
        repos:
          - url: https://github.com/org/user-service.git
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("customAnnotations.controllers");
  }

  @Test
  void failsWhenCustomAnnotationsMappingsIsNotAnObject() {
    final String yaml = """
        customAnnotations:
          mappings: com.acme.fwk.MiGet
        repos:
          - url: https://github.com/org/user-service.git
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("customAnnotations.mappings");
  }

  @Test
  void failsWhenCustomAnnotationsMappingsVerbKeyIsBlank() {
    final String yaml = """
        customAnnotations:
          mappings:
            "": [com.acme.fwk.MiGet]
        repos:
          - url: https://github.com/org/user-service.git
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("customAnnotations.mappings");
  }

  @Test
  void failsWhenCustomAnnotationsMappingsVerbKeyIsNotText() {
    final String yaml = """
        customAnnotations:
          mappings:
            123: [com.acme.fwk.MiGet]
        repos:
          - url: https://github.com/org/user-service.git
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("customAnnotations.mappings");
  }

  @Test
  void failsWhenReposKeyIsMissing() {
    final String yaml = "foo: bar";

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("repos");
  }

  @Test
  void failsWhenAnEntryIsMissingUrl() {
    final String yaml = """
        repos:
          - branch: main
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("url")
        .hasMessageContaining("repos[0]");
  }

  @Test
  void failsWhenProviderCannotBeDeterminedAndIsNotExplicit() {
    final String yaml = """
        repos:
          - url: https://scm.internal.example/org/unknown-service.git
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("proveedor");
  }

  @Test
  void failsWhenBranchIsNotATextValue() {
    final String yaml = """
        repos:
          - url: https://github.com/org/repo.git
            branch: true
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("branch")
        .hasMessageContaining("repos[0]");
  }

  @Test
  void failsWhenExplicitProviderIsUnknown() {
    final String yaml = """
        repos:
          - url: https://github.com/org/repo.git
            provider: bitbucket
        """;

    assertThatThrownBy(() -> loader.parse(new StringReader(yaml)))
        .isInstanceOf(RepoSourceConfigException.class)
        .hasMessageContaining("bitbucket");
  }
}
