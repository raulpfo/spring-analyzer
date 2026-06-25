package io.github.springanalyzer.commands;

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

  @Option(names = {"-o", "--output"}, description = "Output file path (default: report.html)", defaultValue = "report.html")
  private String outputPath;

  /*public void run() {
    progressBar.start("Cloning repositories...", null);
    launchSpringAnalyzeUseCase.run(new CommandConfig(configPath, outputPath));
    progressBar.stop("Analysis complete!");
  }*/
  @Override
  public void run() {
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