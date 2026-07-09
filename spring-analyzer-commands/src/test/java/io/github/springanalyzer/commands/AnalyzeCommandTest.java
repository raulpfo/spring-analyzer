package io.github.springanalyzer.commands;

import io.github.springanalyzer.ui.cli.MultiProgressBar;
import io.github.springanalyzer.ui.cli.ProgressBar;
import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.usecase.LaunchSpringAnalyzeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AnalyzeCommandTest {

  private AnalyzeCommand analyzeCommand;
  private MultiProgressBar multiProgressBar;
  private CommandLine commandLine;

  @BeforeEach
  void setUp() {
    final LaunchSpringAnalyzeUseCase launchSpringAnalyzeUseCase = mock(LaunchSpringAnalyzeUseCase.class);
    final ProgressBar progressBar = mock(ProgressBar.class);
    multiProgressBar = mock(MultiProgressBar.class);

    analyzeCommand = new AnalyzeCommand(launchSpringAnalyzeUseCase, progressBar, multiProgressBar);
    commandLine = new CommandLine(analyzeCommand).setCaseInsensitiveEnumValuesAllowed(true);
  }

  @Test
  void appliesSensibleDefaultsWhenOnlyConfigIsProvided() {
    commandLine.parseArgs("-c", "repos.yml");

    final CommandConfig config = analyzeCommand.toCommandConfig();

    assertThat(config.configPath()).isEqualTo("repos.yml");
    assertThat(config.outputPath()).isEqualTo("report.html");
    assertThat(config.format()).isEqualTo(ReportFormat.HTML);
    assertThat(config.githubToken()).isNull();
    assertThat(config.gitlabToken()).isNull();
    assertThat(config.tokenEnv()).isNull();
    assertThat(config.threads()).isEqualTo(Runtime.getRuntime().availableProcessors());
    assertThat(config.verbose()).isFalse();
    assertThat(config.dryRun()).isFalse();
  }

  @Test
  void parsesAllFlagsWhenExplicitlyProvided() {
    commandLine.parseArgs(
        "-c", "repos.yml",
        "-o", "custom-report.md",
        "--format", "md",
        "--github-token", "gh-token",
        "--gitlab-token", "gl-token",
        "--token-env", "SCM_TOKEN",
        "--threads", "4",
        "--verbose",
        "--dry-run"
    );

    final CommandConfig config = analyzeCommand.toCommandConfig();

    assertThat(config.configPath()).isEqualTo("repos.yml");
    assertThat(config.outputPath()).isEqualTo("custom-report.md");
    assertThat(config.format()).isEqualTo(ReportFormat.MD);
    assertThat(config.githubToken()).isEqualTo("gh-token");
    assertThat(config.gitlabToken()).isEqualTo("gl-token");
    assertThat(config.tokenEnv()).isEqualTo("SCM_TOKEN");
    assertThat(config.threads()).isEqualTo(4);
    assertThat(config.verbose()).isTrue();
    assertThat(config.dryRun()).isTrue();
  }

  @Test
  void requiresConfigFlag() {
    org.junit.jupiter.api.Assertions.assertThrows(
        CommandLine.MissingParameterException.class,
        () -> commandLine.parseArgs()
    );
  }

  @Test
  void rejectsUnknownFormatValue() {
    org.junit.jupiter.api.Assertions.assertThrows(
        CommandLine.ParameterException.class,
        () -> commandLine.parseArgs("-c", "repos.yml", "--format", "yaml")
    );
  }

  @Test
  void dryRunSkipsCloningAndAnalysis() {
    commandLine.parseArgs("-c", "repos.yml", "--dry-run");

    analyzeCommand.run();

    verifyNoInteractions(multiProgressBar);
  }
}
