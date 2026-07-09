package io.github.springanalyzer.core.analyzer;

public interface LanguageAnalyzer {

  boolean supports(RepoContext context);

  ServiceAnalysisResult analyze(RepoContext context);
}
