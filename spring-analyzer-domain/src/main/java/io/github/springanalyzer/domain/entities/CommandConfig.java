package io.github.springanalyzer.domain.entities;

public record CommandConfig(
    String configPath,
    String outputPath,
    ReportFormat format,
    String githubToken,
    String gitlabToken,
    String tokenEnv,
    int threads,
    boolean verbose,
    boolean dryRun
) {
}