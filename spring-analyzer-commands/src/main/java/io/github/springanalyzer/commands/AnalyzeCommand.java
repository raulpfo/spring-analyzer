package io.github.springanalyzer.commands;

import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.usecase.LaunchSpringAnalyzeUseCase;
import io.github.springanalyzer.commands.ui.ProgressBar;
import io.github.springanalyzer.commands.ui.MultiProgressBar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;
import java.util.Random;
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

  @Option(names = {"-c", "--config"}, description = "Path to repos.yml config file", required = true)
  private String configPath;

  @Option(names = {"-o", "--output"}, description = "Output file path", defaultValue = "report.html")
  private String outputPath;

  @Option(names = {"--format"}, description = "Report format: ${COMPLETION-CANDIDATES}", defaultValue = "HTML")
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

  CommandConfig toCommandConfig() {
    return new CommandConfig(configPath, outputPath, format, githubToken, gitlabToken, tokenEnv, resolveThreads(), verbose, dryRun);
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

    final List<String> repos = List.of("Cloning user-service", "Cloning order-service", "Cloning auth-service");

    multiProgressBar.start(repos);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      final List<? extends Future<?>> futures = repos.stream()
          .map(repo -> executor.submit(() -> {
            try {
              // Simulamos trabajo con sleeps distintos
              Thread.sleep(new Random().nextLong(1000, 10000));
              multiProgressBar.done(repo);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              multiProgressBar.error(repo);
            }
          }))
          .toList();

      for (Future<?> future : futures) {
        future.get();
      }
    } catch (Exception e) {
      Thread.currentThread().interrupt();
    }

    multiProgressBar.stop();
  }
}