package io.github.springanalyzer.commands;

import io.github.springanalyzer.core.config.RepoSourceConfigLoader;
import io.github.springanalyzer.domain.entities.RepoDefinition;
import io.github.springanalyzer.domain.entities.RepoSourceConfig;
import io.github.springanalyzer.domain.entities.ScmProvider;
import io.github.springanalyzer.scm.git.GitCloner;
import io.github.springanalyzer.ui.cli.MultiProgressBar;
import io.github.springanalyzer.ui.cli.ProgressBar;
import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.ReportFormat;
import io.github.springanalyzer.domain.usecase.LaunchSpringAnalyzeUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnalyzeCommandTest {

  private AnalyzeCommand analyzeCommand;
  private MultiProgressBar multiProgressBar;
  private RepoSourceConfigLoader repoSourceConfigLoader;
  private GitCloner gitCloner;
  private CommandLine commandLine;

  @BeforeEach
  void setUp() {
    final LaunchSpringAnalyzeUseCase launchSpringAnalyzeUseCase = mock(LaunchSpringAnalyzeUseCase.class);
    final ProgressBar progressBar = mock(ProgressBar.class);
    multiProgressBar = mock(MultiProgressBar.class);
    repoSourceConfigLoader = mock(RepoSourceConfigLoader.class);
    gitCloner = mock(GitCloner.class);

    analyzeCommand = new AnalyzeCommand(launchSpringAnalyzeUseCase, progressBar, multiProgressBar, repoSourceConfigLoader, gitCloner);
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
  void dryRunSkipsCloningAndAnalysis() {
    commandLine.parseArgs("-c", "repos.yml", "--dry-run");

    analyzeCommand.run();

    verifyNoInteractions(multiProgressBar);
    verifyNoInteractions(repoSourceConfigLoader);
    verifyNoInteractions(gitCloner);
  }

  @Test
  void clonesEachConfiguredRepoAndReportsProgress() {
    final RepoDefinition userService = new RepoDefinition("https://github.com/acme/user-service.git", null, ScmProvider.GITHUB);
    final RepoDefinition orderService = new RepoDefinition("https://gitlab.com/acme/order-service.git", null, ScmProvider.GITLAB);
    when(repoSourceConfigLoader.load(any())).thenReturn(new RepoSourceConfig(java.util.List.of(userService, orderService)));

    final Path userServiceDir = Path.of("/tmp/user-service-clone");
    final Path orderServiceDir = Path.of("/tmp/order-service-clone");
    when(gitCloner.clone(eq(userService), any())).thenReturn(userServiceDir);
    when(gitCloner.clone(eq(orderService), any())).thenReturn(orderServiceDir);

    commandLine.parseArgs("-c", "repos.yml", "--github-token", "gh-token", "--gitlab-token", "gl-token");

    analyzeCommand.run();

    verify(multiProgressBar).start(java.util.List.of("user-service", "order-service"));
    verify(gitCloner).clone(userService, Optional.of("gh-token"));
    verify(gitCloner).clone(orderService, Optional.of("gl-token"));
    verify(multiProgressBar).done("user-service");
    verify(multiProgressBar).done("order-service");
  }

  @Test
  void cleansUpClonedDirectoriesByDefault() {
    final RepoDefinition userService = new RepoDefinition("https://github.com/acme/user-service.git", null, ScmProvider.GITHUB);
    when(repoSourceConfigLoader.load(any())).thenReturn(new RepoSourceConfig(java.util.List.of(userService)));
    final Path clonedDir = Path.of("/tmp/user-service-clone");
    when(gitCloner.clone(eq(userService), any())).thenReturn(clonedDir);

    commandLine.parseArgs("-c", "repos.yml");

    analyzeCommand.run();

    verify(gitCloner).cleanup(clonedDir);
  }

  @Test
  void keepsClonedDirectoriesWhenKeepTempDirsFlagIsSet() {
    final RepoDefinition userService = new RepoDefinition("https://github.com/acme/user-service.git", null, ScmProvider.GITHUB);
    when(repoSourceConfigLoader.load(any())).thenReturn(new RepoSourceConfig(java.util.List.of(userService)));
    final Path clonedDir = Path.of("/tmp/user-service-clone");
    when(gitCloner.clone(eq(userService), any())).thenReturn(clonedDir);

    commandLine.parseArgs("-c", "repos.yml", "--keep-temp-dirs");

    analyzeCommand.run();

    verify(gitCloner, never()).cleanup(any());
  }

  @Test
  void marksRepoAsErrorWhenCloningFails() {
    final RepoDefinition brokenService = new RepoDefinition("https://github.com/acme/broken-service.git", null, ScmProvider.GITHUB);
    when(repoSourceConfigLoader.load(any())).thenReturn(new RepoSourceConfig(java.util.List.of(brokenService)));
    doThrow(new RuntimeException("clone failed")).when(gitCloner).clone(eq(brokenService), any());

    commandLine.parseArgs("-c", "repos.yml");

    analyzeCommand.run();

    verify(multiProgressBar).error("broken-service");
    verify(multiProgressBar, never()).done("broken-service");
  }
}
