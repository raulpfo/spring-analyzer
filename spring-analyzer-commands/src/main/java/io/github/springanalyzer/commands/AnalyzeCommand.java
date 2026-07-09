package io.github.springanalyzer.commands;

import io.github.springanalyzer.core.config.RepoSourceConfigLoader;
import io.github.springanalyzer.domain.credentials.CredentialResolver;
import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.RepoDefinition;
import io.github.springanalyzer.domain.entities.RepoSourceConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.usecase.LaunchSpringAnalyzeUseCase;
import io.github.springanalyzer.scm.git.GitCloner;
import io.github.springanalyzer.ui.cli.ProgressBar;
import io.github.springanalyzer.ui.cli.MultiProgressBar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Command(
    name = "spring-analyzer",
    description = "Analyzes Spring Boot microservice architectures and generates reports",
    mixinStandardHelpOptions = true,
    version = "0.1.0"
)
@RequiredArgsConstructor
public class AnalyzeCommand implements Runnable {

  private final LaunchSpringAnalyzeUseCase launchSpringAnalyzeUseCase;

  private final ProgressBar progressBar;

  private final MultiProgressBar multiProgressBar;

  private final RepoSourceConfigLoader repoSourceConfigLoader;

  private final GitCloner gitCloner;

  @Option(names = {"-c", "--config"}, description = "Path to repos.yml config file", required = true)
  private String configPath;

  @Option(names = {"-o", "--output"}, description = "Output file path", defaultValue = "report.html", showDefaultValue = Visibility.ALWAYS)
  private String outputPath;

  @Option(names = {"--format"}, description = "Report format: ${COMPLETION-CANDIDATES}", defaultValue = "HTML", showDefaultValue = Visibility.ALWAYS)
  private ReportFormat format;

  @Option(names = {"--github-token"}, description = "GitHub token used to clone private repositories")
  private String githubToken;

  @Option(names = {"--gitlab-token"}, description = "GitLab token used to clone private repositories")
  private String gitlabToken;

  @Option(names = {"--token-env"}, description = "Name of the environment variable holding the SCM token, as an alternative to --github-token/--gitlab-token")
  private String tokenEnv;

  @Option(names = {"--threads"}, description = "Number of concurrent threads used to clone and analyze repositories (default: number of available processors)")
  private int threads = -1;

  @Option(names = {"-v", "--verbose"}, description = "Enable verbose output")
  private boolean verbose;

  @Option(names = {"--dry-run"}, description = "Print the resolved configuration without cloning or analyzing anything")
  private boolean dryRun;

  @Option(names = {"--keep-temp-dirs"}, description = "Keep the cloned repositories' temporary directories after the analysis (useful for debugging)")
  private boolean keepTempDirs;

  CommandConfig toCommandConfig() {
    return new CommandConfig(configPath, outputPath, format, githubToken, gitlabToken, tokenEnv, resolveThreads(), verbose, dryRun, keepTempDirs);
  }

  private int resolveThreads() {
    return threads > 0 ? threads : Runtime.getRuntime().availableProcessors();
  }

  @Override
  public void run() {
    final CommandConfig config = toCommandConfig();

    if (verbose || dryRun) {
      System.out.println("Configuration: " + config);
    }

    if (dryRun) {
      return;
    }

    final RepoSourceConfig repoSourceConfig = repoSourceConfigLoader.load(Path.of(config.configPath()));
    final CredentialResolver credentialResolver = new CredentialResolver(config);
    final List<RepoDefinition> repos = repoSourceConfig.repos();

    multiProgressBar.start(repos.stream().map(RepoDefinition::repoName).toList());

    final Map<String, Path> clonedDirectories = new ConcurrentHashMap<>();

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final List<? extends Future<?>> futures = repos.stream()
          .map(repo -> executor.submit(() -> cloneRepo(repo, credentialResolver, clonedDirectories)))
          .toList();

      for (Future<?> future : futures) {
        future.get();
      }
    } catch (Exception e) {
      Thread.currentThread().interrupt();
    }

    multiProgressBar.stop();

    if (!config.keepTempDirs()) {
      clonedDirectories.values().forEach(gitCloner::cleanup);
    }
  }

  private void cloneRepo(final RepoDefinition repo, final CredentialResolver credentialResolver, final Map<String, Path> clonedDirectories) {
    final String name = repo.repoName();
    try {
      final Optional<String> token = credentialResolver.resolve(repo.provider());
      final Path clonedDir = gitCloner.clone(repo, token);
      clonedDirectories.put(name, clonedDir);
      multiProgressBar.done(name);
    } catch (Exception e) {
      multiProgressBar.error(name);
    }
  }
}
