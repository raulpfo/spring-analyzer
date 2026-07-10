package io.github.springanalyzer.commands;

import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.usecase.LaunchSpringAnalyzeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalyzeCommandTest {

  private AnalyzeCommand analyzeCommand;
  private LaunchSpringAnalyzeUseCase launchSpringAnalyzeUseCase;
  private CommandLine commandLine;

  @BeforeEach
  void setUp() {
    launchSpringAnalyzeUseCase = mock(LaunchSpringAnalyzeUseCase.class);

    analyzeCommand = new AnalyzeCommand(launchSpringAnalyzeUseCase);
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
    assertThat(config.keepTempDirs()).isFalse();
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
        "--dry-run",
        "--keep-temp-dirs"
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
    assertThat(config.keepTempDirs()).isTrue();
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
  void dryRunSkipsInvokingTheUseCase() {
    commandLine.parseArgs("-c", "repos.yml", "--dry-run");

    analyzeCommand.run();

    verifyNoInteractions(launchSpringAnalyzeUseCase);
  }

  @Test
  void delegatesToTheUseCaseWithTheResolvedConfig() {
    when(launchSpringAnalyzeUseCase.run(any())).thenReturn("Analisis completado");

    commandLine.parseArgs("-c", "repos.yml", "--github-token", "gh-token");

    analyzeCommand.run();

    verify(launchSpringAnalyzeUseCase).run(analyzeCommand.toCommandConfig());
  }
}
