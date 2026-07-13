package io.github.springanalyzer.application.usecase;

import io.github.springanalyzer.analyzers.ServiceSnapshotBuilder;
import io.github.springanalyzer.core.analyzer.AnalysisOutcome;
import io.github.springanalyzer.core.analyzer.AnalyzerRegistry;
import io.github.springanalyzer.core.analyzer.DependencyGraph;
import io.github.springanalyzer.core.analyzer.DependencyGraphBuilder;
import io.github.springanalyzer.core.analyzer.RepoContext;
import io.github.springanalyzer.core.analyzer.ServiceSnapshot;
import io.github.springanalyzer.core.config.RepoSourceConfigLoader;
import io.github.springanalyzer.domain.credentials.CredentialResolver;
import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.CustomAnnotationsConfig;
import io.github.springanalyzer.domain.entities.RepoDefinition;
import io.github.springanalyzer.domain.entities.RepoSourceConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.usecase.LaunchSpringAnalyzeUseCase;
import io.github.springanalyzer.reporter.HtmlReportGenerator;
import io.github.springanalyzer.scm.git.GitCloner;
import io.github.springanalyzer.ui.cli.MultiProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class LaunchSpringAnalyzeUseCaseImpl implements LaunchSpringAnalyzeUseCase {

  private static final Logger LOGGER = LoggerFactory.getLogger(LaunchSpringAnalyzeUseCaseImpl.class);

  private final RepoSourceConfigLoader repoSourceConfigLoader;
  private final GitCloner gitCloner;
  private final MultiProgressBar multiProgressBar;
  private final AnalyzerRegistry analyzerRegistry;
  private final ServiceSnapshotBuilder serviceSnapshotBuilder;
  private final DependencyGraphBuilder dependencyGraphBuilder;
  private final HtmlReportGenerator htmlReportGenerator;

  public LaunchSpringAnalyzeUseCaseImpl(final RepoSourceConfigLoader repoSourceConfigLoader, final GitCloner gitCloner,
      final MultiProgressBar multiProgressBar, final AnalyzerRegistry analyzerRegistry,
      final ServiceSnapshotBuilder serviceSnapshotBuilder, final DependencyGraphBuilder dependencyGraphBuilder,
      final HtmlReportGenerator htmlReportGenerator) {
    this.repoSourceConfigLoader = repoSourceConfigLoader;
    this.gitCloner = gitCloner;
    this.multiProgressBar = multiProgressBar;
    this.analyzerRegistry = analyzerRegistry;
    this.serviceSnapshotBuilder = serviceSnapshotBuilder;
    this.dependencyGraphBuilder = dependencyGraphBuilder;
    this.htmlReportGenerator = htmlReportGenerator;
  }

  @Override
  public String run(final CommandConfig command) {
    if (command.format() != ReportFormat.HTML) {
      throw new UnsupportedReportFormatException(command.format());
    }

    final RepoSourceConfig repoSourceConfig = repoSourceConfigLoader.load(Path.of(command.configPath()));
    final CredentialResolver credentialResolver = new CredentialResolver(command);
    final List<RepoDefinition> repos = repoSourceConfig.repos();
    final CustomAnnotationsConfig customAnnotations = repoSourceConfig.customAnnotations();

    multiProgressBar.start(repos.stream().map(RepoDefinition::repoName).toList());

    final Map<String, Path> clonedDirectories = new ConcurrentHashMap<>();
    final List<ServiceSnapshot> snapshots = new CopyOnWriteArrayList<>();
    final List<String> unsupportedRepos = new CopyOnWriteArrayList<>();
    final List<String> failedRepos = new CopyOnWriteArrayList<>();

    try (ExecutorService executor = Executors.newFixedThreadPool(command.threads())) {
      final List<Callable<Void>> tasks = repos.stream()
          .<Callable<Void>>map(repo -> () -> {
            processRepo(repo, credentialResolver, customAnnotations, clonedDirectories, snapshots, unsupportedRepos,
                failedRepos);
            return null;
          })
          .toList();
      executor.invokeAll(tasks);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    multiProgressBar.stop();

    if (!command.keepTempDirs()) {
      clonedDirectories.values().forEach(gitCloner::cleanup);
    }

    final DependencyGraph graph = dependencyGraphBuilder.build(snapshots);
    final String html = htmlReportGenerator.generate(graph);
    writeReport(Path.of(command.outputPath()), html);

    return summaryOf(repos.size(), snapshots.size(), unsupportedRepos, failedRepos, command.outputPath());
  }

  private void processRepo(final RepoDefinition repo, final CredentialResolver credentialResolver,
      final CustomAnnotationsConfig customAnnotations, final Map<String, Path> clonedDirectories,
      final List<ServiceSnapshot> snapshots, final List<String> unsupportedRepos, final List<String> failedRepos) {
    final String repoName = repo.repoName();
    try {
      final Optional<String> token = credentialResolver.resolve(repo.provider());
      final Path clonedDir = gitCloner.clone(repo, token);
      clonedDirectories.put(repoName, clonedDir);

      final RepoContext context = new RepoContext(repoName, clonedDir);
      final AnalysisOutcome outcome = analyzerRegistry.dispatch(context);
      if (outcome instanceof AnalysisOutcome.Analyzed) {
        snapshots.add(serviceSnapshotBuilder.build(context, customAnnotations));
      } else {
        unsupportedRepos.add(repoName);
      }
      multiProgressBar.done(repoName);
    } catch (final Exception e) {
      LOGGER.warn("No se pudo analizar el repositorio {}", repoName, e);
      failedRepos.add(repoName);
      multiProgressBar.error(repoName);
    }
  }

  private void writeReport(final Path outputPath, final String content) {
    try {
      Files.writeString(outputPath, content, StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException("No se pudo escribir el reporte en " + outputPath, e);
    }
  }

  private String summaryOf(final int totalRepos, final int analyzedCount, final List<String> unsupportedRepos,
      final List<String> failedRepos, final String outputPath) {
    return "Analisis completado: " + totalRepos + " repositorio(s), " + analyzedCount + " analizado(s), "
        + unsupportedRepos.size() + " no soportado(s), " + failedRepos.size() + " fallido(s). "
        + "Reporte generado en " + outputPath;
  }
}
