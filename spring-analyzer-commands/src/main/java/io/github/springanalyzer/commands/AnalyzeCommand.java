package io.github.springanalyzer.commands;

import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.usecase.LaunchSpringAnalyzeUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

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

    final String result = launchSpringAnalyzeUseCase.run(config);
    System.out.println(result);
  }
}
