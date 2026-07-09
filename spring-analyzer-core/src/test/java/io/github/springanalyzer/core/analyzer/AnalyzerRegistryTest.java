package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalyzerRegistryTest {

  @Test
  void dispatchesToTheAnalyzerThatSupportsTheRepo(@TempDir final Path repoDir) throws IOException {
    Files.createFile(repoDir.resolve("pom.xml"));
    final RepoContext context = new RepoContext("user-service", repoDir);
    final AnalyzerRegistry registry = new AnalyzerRegistry(List.of(new MarkerFileAnalyzer("pom.xml", "java"),
        new MarkerFileAnalyzer("package.json", "node")));

    final AnalysisOutcome outcome = registry.dispatch(context);

    assertThat(outcome).isInstanceOf(AnalysisOutcome.Analyzed.class);
    final ServiceAnalysisResult result = ((AnalysisOutcome.Analyzed) outcome).result();
    assertThat(result.repoName()).isEqualTo("user-service");
    assertThat(result.language()).isEqualTo("java");
  }

  @Test
  void dispatchesToTheMatchingAnalyzerAmongSeveralSupportedTypes(@TempDir final Path repoDir) throws IOException {
    Files.createFile(repoDir.resolve("package.json"));
    final RepoContext context = new RepoContext("frontend-service", repoDir);
    final AnalyzerRegistry registry = new AnalyzerRegistry(List.of(new MarkerFileAnalyzer("pom.xml", "java"),
        new MarkerFileAnalyzer("package.json", "node")));

    final AnalysisOutcome outcome = registry.dispatch(context);

    assertThat(outcome).isInstanceOf(AnalysisOutcome.Analyzed.class);
    assertThat(((AnalysisOutcome.Analyzed) outcome).result().language()).isEqualTo("node");
  }

  @Test
  void marksRepoAsUnsupportedWhenNoAnalyzerMatchesWithoutThrowing(@TempDir final Path repoDir) throws IOException {
    Files.createFile(repoDir.resolve("Gemfile"));
    final RepoContext context = new RepoContext("legacy-service", repoDir);
    final AnalyzerRegistry registry = new AnalyzerRegistry(List.of(new MarkerFileAnalyzer("pom.xml", "java"),
        new MarkerFileAnalyzer("package.json", "node")));

    final AnalysisOutcome outcome = registry.dispatch(context);

    assertThat(outcome).isInstanceOf(AnalysisOutcome.Unsupported.class);
    assertThat(((AnalysisOutcome.Unsupported) outcome).repoContext()).isEqualTo(context);
  }

  @Test
  void marksRepoAsUnsupportedWhenNoAnalyzersAreRegistered(@TempDir final Path repoDir) {
    final RepoContext context = new RepoContext("empty-service", repoDir);
    final AnalyzerRegistry registry = new AnalyzerRegistry(List.of());

    final AnalysisOutcome outcome = registry.dispatch(context);

    assertThat(outcome).isInstanceOf(AnalysisOutcome.Unsupported.class);
  }

  @Test
  void doesNotInvokeAnalyzersThatDoNotSupportTheRepo(@TempDir final Path repoDir) {
    final RepoContext context = new RepoContext("user-service", repoDir);
    final LanguageAnalyzer unsupportingAnalyzer = mock(LanguageAnalyzer.class);
    when(unsupportingAnalyzer.supports(context)).thenReturn(false);
    final LanguageAnalyzer supportingAnalyzer = mock(LanguageAnalyzer.class);
    when(supportingAnalyzer.supports(context)).thenReturn(true);
    when(supportingAnalyzer.analyze(context)).thenReturn(new ServiceAnalysisResult("user-service", "java"));
    final AnalyzerRegistry registry = new AnalyzerRegistry(List.of(unsupportingAnalyzer, supportingAnalyzer));

    registry.dispatch(context);

    verify(unsupportingAnalyzer, never()).analyze(any());
  }

  private static final class MarkerFileAnalyzer implements LanguageAnalyzer {

    private final String markerFileName;
    private final String language;

    private MarkerFileAnalyzer(final String markerFileName, final String language) {
      this.markerFileName = markerFileName;
      this.language = language;
    }

    @Override
    public boolean supports(final RepoContext context) {
      return Files.exists(context.localPath().resolve(markerFileName));
    }

    @Override
    public ServiceAnalysisResult analyze(final RepoContext context) {
      return new ServiceAnalysisResult(context.repoName(), language);
    }
  }
}
