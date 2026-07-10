package io.github.springanalyzer.analyzers.spring;

import io.github.springanalyzer.core.analyzer.LanguageAnalyzer;
import io.github.springanalyzer.core.analyzer.RepoContext;
import io.github.springanalyzer.core.analyzer.ServiceAnalysisResult;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class SpringJavaLanguageAnalyzer implements LanguageAnalyzer {

  private static final String LANGUAGE = "java";

  @Override
  public boolean supports(final RepoContext context) {
    final Path root = context.localPath();
    return Files.isRegularFile(root.resolve("pom.xml"))
        || Files.isRegularFile(root.resolve("build.gradle"))
        || Files.isRegularFile(root.resolve("build.gradle.kts"));
  }

  @Override
  public ServiceAnalysisResult analyze(final RepoContext context) {
    return new ServiceAnalysisResult(context.repoName(), LANGUAGE);
  }
}
