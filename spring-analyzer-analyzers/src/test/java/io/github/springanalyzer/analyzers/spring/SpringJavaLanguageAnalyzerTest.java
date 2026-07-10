package io.github.springanalyzer.analyzers.spring;

import io.github.springanalyzer.core.analyzer.RepoContext;
import io.github.springanalyzer.core.analyzer.ServiceAnalysisResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SpringJavaLanguageAnalyzerTest {

  private final SpringJavaLanguageAnalyzer analyzer = new SpringJavaLanguageAnalyzer();

  @Test
  void supportsARepoWithAPomXml(@TempDir final Path repoDir) throws IOException {
    Files.createFile(repoDir.resolve("pom.xml"));

    assertThat(analyzer.supports(new RepoContext("order-service", repoDir))).isTrue();
  }

  @Test
  void supportsARepoWithABuildGradle(@TempDir final Path repoDir) throws IOException {
    Files.createFile(repoDir.resolve("build.gradle"));

    assertThat(analyzer.supports(new RepoContext("order-service", repoDir))).isTrue();
  }

  @Test
  void supportsARepoWithABuildGradleKts(@TempDir final Path repoDir) throws IOException {
    Files.createFile(repoDir.resolve("build.gradle.kts"));

    assertThat(analyzer.supports(new RepoContext("order-service", repoDir))).isTrue();
  }

  @Test
  void doesNotSupportARepoWithoutAKnownBuildFile(@TempDir final Path repoDir) throws IOException {
    Files.createFile(repoDir.resolve("Gemfile"));

    assertThat(analyzer.supports(new RepoContext("order-service", repoDir))).isFalse();
  }

  @Test
  void analyzeReturnsTheRepoNameTaggedAsJava(@TempDir final Path repoDir) {
    final ServiceAnalysisResult result = analyzer.analyze(new RepoContext("order-service", repoDir));

    assertThat(result.repoName()).isEqualTo("order-service");
    assertThat(result.language()).isEqualTo("java");
  }
}
