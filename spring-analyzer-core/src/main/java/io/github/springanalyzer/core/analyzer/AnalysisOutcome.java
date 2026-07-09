package io.github.springanalyzer.core.analyzer;

public sealed interface AnalysisOutcome {

  record Analyzed(ServiceAnalysisResult result) implements AnalysisOutcome {
  }

  record Unsupported(RepoContext repoContext) implements AnalysisOutcome {
  }
}
