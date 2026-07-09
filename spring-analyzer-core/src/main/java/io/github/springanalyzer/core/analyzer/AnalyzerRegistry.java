package io.github.springanalyzer.core.analyzer;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalyzerRegistry {

  private final List<LanguageAnalyzer> analyzers;

  public AnalyzerRegistry(final List<LanguageAnalyzer> analyzers) {
    this.analyzers = List.copyOf(analyzers);
  }

  public AnalysisOutcome dispatch(final RepoContext context) {
    return analyzers.stream()
        .filter(analyzer -> analyzer.supports(context))
        .findFirst()
        .<AnalysisOutcome>map(analyzer -> new AnalysisOutcome.Analyzed(analyzer.analyze(context)))
        .orElseGet(() -> new AnalysisOutcome.Unsupported(context));
  }
}
