package io.github.springanalyzer.application.usecase;

import io.github.springanalyzer.analyzers.ServiceSnapshotBuilder;
import io.github.springanalyzer.core.analyzer.AnalysisOutcome;
import io.github.springanalyzer.core.analyzer.AnalyzerRegistry;
import io.github.springanalyzer.core.analyzer.DependencyGraph;
import io.github.springanalyzer.core.analyzer.DependencyGraphBuilder;
import io.github.springanalyzer.core.analyzer.RepoContext;
import io.github.springanalyzer.core.analyzer.ServiceAnalysisResult;
import io.github.springanalyzer.core.analyzer.ServiceSnapshot;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;
import io.github.springanalyzer.core.config.RepoSourceConfigLoader;
import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.RepoDefinition;
import io.github.springanalyzer.domain.entities.RepoSourceConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.entities.ScmProvider;
import io.github.springanalyzer.reporter.HtmlReportGenerator;
import io.github.springanalyzer.scm.git.GitCloner;
import io.github.springanalyzer.ui.cli.MultiProgressBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LaunchSpringAnalyzeUseCaseImplTest {

  private RepoSourceConfigLoader repoSourceConfigLoader;
  private GitCloner gitCloner;
  private MultiProgressBar multiProgressBar;
  private AnalyzerRegistry analyzerRegistry;
  private ServiceSnapshotBuilder serviceSnapshotBuilder;
  private DependencyGraphBuilder dependencyGraphBuilder;
  private HtmlReportGenerator htmlReportGenerator;
  private LaunchSpringAnalyzeUseCaseImpl useCase;

  private final RepoDefinition orderService =
      new RepoDefinition("https://github.com/acme/order-service.git", null, ScmProvider.GITHUB);
  private final RepoDefinition userService =
      new RepoDefinition("https://github.com/acme/user-service.git", null, ScmProvider.GITHUB);

  @BeforeEach
  void setUp() {
    repoSourceConfigLoader = mock(RepoSourceConfigLoader.class);
    gitCloner = mock(GitCloner.class);
    multiProgressBar = mock(MultiProgressBar.class);
    analyzerRegistry = mock(AnalyzerRegistry.class);
    serviceSnapshotBuilder = mock(ServiceSnapshotBuilder.class);
    dependencyGraphBuilder = mock(DependencyGraphBuilder.class);
    htmlReportGenerator = mock(HtmlReportGenerator.class);
    useCase = new LaunchSpringAnalyzeUseCaseImpl(repoSourceConfigLoader, gitCloner, multiProgressBar, analyzerRegistry,
        serviceSnapshotBuilder, dependencyGraphBuilder, htmlReportGenerator);
  }

  private static CommandConfig commandConfig(final String outputPath, final boolean keepTempDirs) {
    return new CommandConfig("repos.yml", outputPath, ReportFormat.HTML, null, null, null, 2, false, false, keepTempDirs);
  }

  @Test
  void analyzesEachRepoBuildsTheGraphAndWritesTheReport(@TempDir final Path tempDir) throws IOException {
    final Path outputPath = tempDir.resolve("report.html");
    when(repoSourceConfigLoader.load(any())).thenReturn(new RepoSourceConfig(List.of(orderService, userService)));

    final Path orderDir = tempDir.resolve("order-clone");
    final Path userDir = tempDir.resolve("user-clone");
    when(gitCloner.clone(orderService, Optional.empty())).thenReturn(orderDir);
    when(gitCloner.clone(userService, Optional.empty())).thenReturn(userDir);

    final RepoContext orderContext = new RepoContext("order-service", orderDir);
    final RepoContext userContext = new RepoContext("user-service", userDir);
    when(analyzerRegistry.dispatch(orderContext))
        .thenReturn(new AnalysisOutcome.Analyzed(new ServiceAnalysisResult("order-service", "java")));
    when(analyzerRegistry.dispatch(userContext))
        .thenReturn(new AnalysisOutcome.Analyzed(new ServiceAnalysisResult("user-service", "java")));

    final ServiceSnapshot orderSnapshot =
        new ServiceSnapshot("order-service", List.of(), List.of(), ServiceVersionInfo.unknown());
    final ServiceSnapshot userSnapshot =
        new ServiceSnapshot("user-service", List.of(), List.of(), ServiceVersionInfo.unknown());
    when(serviceSnapshotBuilder.build(orderContext)).thenReturn(orderSnapshot);
    when(serviceSnapshotBuilder.build(userContext)).thenReturn(userSnapshot);

    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(), List.of(), List.of());
    when(dependencyGraphBuilder.build(anyList())).thenReturn(graph);
    when(htmlReportGenerator.generate(graph)).thenReturn("<html>report</html>");

    final String result = useCase.run(commandConfig(outputPath.toString(), false));

    assertThat(Files.readString(outputPath)).isEqualTo("<html>report</html>");
    assertThat(result).contains("2 repositorio(s)").contains("2 analizado(s)").contains("0 no soportado(s)")
        .contains("0 fallido(s)");
    verify(multiProgressBar).start(List.of("order-service", "user-service"));
    verify(multiProgressBar).done("order-service");
    verify(multiProgressBar).done("user-service");
    verify(multiProgressBar).stop();
    verify(gitCloner).cleanup(orderDir);
    verify(gitCloner).cleanup(userDir);
  }

  @Test
  void aFailedCloneDoesNotAbortTheAnalysisOfOtherRepos(@TempDir final Path tempDir) throws IOException {
    final Path outputPath = tempDir.resolve("report.html");
    when(repoSourceConfigLoader.load(any())).thenReturn(new RepoSourceConfig(List.of(orderService, userService)));

    final Path userDir = tempDir.resolve("user-clone");
    when(gitCloner.clone(orderService, Optional.empty())).thenThrow(new RuntimeException("clone failed"));
    when(gitCloner.clone(userService, Optional.empty())).thenReturn(userDir);

    final RepoContext userContext = new RepoContext("user-service", userDir);
    when(analyzerRegistry.dispatch(userContext))
        .thenReturn(new AnalysisOutcome.Analyzed(new ServiceAnalysisResult("user-service", "java")));
    final ServiceSnapshot userSnapshot =
        new ServiceSnapshot("user-service", List.of(), List.of(), ServiceVersionInfo.unknown());
    when(serviceSnapshotBuilder.build(userContext)).thenReturn(userSnapshot);

    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(), List.of(), List.of());
    when(dependencyGraphBuilder.build(List.of(userSnapshot))).thenReturn(graph);
    when(htmlReportGenerator.generate(graph)).thenReturn("<html>report</html>");

    final String result = useCase.run(commandConfig(outputPath.toString(), false));

    assertThat(result).contains("2 repositorio(s)").contains("1 analizado(s)").contains("1 fallido(s)");
    verify(multiProgressBar).error("order-service");
    verify(multiProgressBar).done("user-service");
    verify(gitCloner).cleanup(userDir);
    verify(gitCloner, times(1)).cleanup(any());
  }

  @Test
  void anUnsupportedRepoIsExcludedFromTheGraphButDoesNotCountAsAFailure(@TempDir final Path tempDir) throws IOException {
    final Path outputPath = tempDir.resolve("report.html");
    when(repoSourceConfigLoader.load(any())).thenReturn(new RepoSourceConfig(List.of(orderService)));

    final Path orderDir = tempDir.resolve("order-clone");
    when(gitCloner.clone(orderService, Optional.empty())).thenReturn(orderDir);

    final RepoContext orderContext = new RepoContext("order-service", orderDir);
    when(analyzerRegistry.dispatch(orderContext)).thenReturn(new AnalysisOutcome.Unsupported(orderContext));

    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(), List.of(), List.of());
    when(dependencyGraphBuilder.build(List.of())).thenReturn(graph);
    when(htmlReportGenerator.generate(graph)).thenReturn("<html>report</html>");

    final String result = useCase.run(commandConfig(outputPath.toString(), false));

    assertThat(result).contains("1 repositorio(s)").contains("0 analizado(s)").contains("1 no soportado(s)")
        .contains("0 fallido(s)");
    verify(multiProgressBar).done("order-service");
    verify(multiProgressBar, never()).error(any());
  }

  @Test
  void keepsClonedDirectoriesWhenKeepTempDirsIsSet(@TempDir final Path tempDir) throws IOException {
    final Path outputPath = tempDir.resolve("report.html");
    when(repoSourceConfigLoader.load(any())).thenReturn(new RepoSourceConfig(List.of(orderService)));

    final Path orderDir = tempDir.resolve("order-clone");
    when(gitCloner.clone(orderService, Optional.empty())).thenReturn(orderDir);

    final RepoContext orderContext = new RepoContext("order-service", orderDir);
    when(analyzerRegistry.dispatch(orderContext)).thenReturn(new AnalysisOutcome.Unsupported(orderContext));

    final DependencyGraph graph = new DependencyGraph(List.of(), List.of(), List.of(), List.of());
    when(dependencyGraphBuilder.build(List.of())).thenReturn(graph);
    when(htmlReportGenerator.generate(graph)).thenReturn("<html>report</html>");

    useCase.run(commandConfig(outputPath.toString(), true));

    verify(gitCloner, never()).cleanup(any());
  }

  @Test
  void rejectsUnsupportedReportFormatsWithoutTouchingAnyCollaborator() {
    final CommandConfig command =
        new CommandConfig("repos.yml", "report.md", ReportFormat.MD, null, null, null, 2, false, false, false);

    assertThatThrownBy(() -> useCase.run(command)).isInstanceOf(UnsupportedOperationException.class);

    verifyNoInteractions(repoSourceConfigLoader, gitCloner, multiProgressBar, analyzerRegistry, serviceSnapshotBuilder,
        dependencyGraphBuilder, htmlReportGenerator);
  }
}
