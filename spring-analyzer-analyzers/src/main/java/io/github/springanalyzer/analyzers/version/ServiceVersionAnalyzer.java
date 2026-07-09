package io.github.springanalyzer.analyzers.version;

import io.github.springanalyzer.analyzers.AnalysisException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class ServiceVersionAnalyzer {

  public ServiceVersionInfo analyze(final Path repoRoot) {
    final Path pom = repoRoot.resolve("pom.xml");
    if (Files.isRegularFile(pom)) {
      return parsePom(readFile(pom));
    }
    final Path buildGradle = repoRoot.resolve("build.gradle");
    if (Files.isRegularFile(buildGradle)) {
      return parseGradle(readFile(buildGradle));
    }
    return ServiceVersionInfo.unknown();
  }

  public ServiceVersionInfo parsePom(final String pomXml) {
    return MavenPomParser.parse(pomXml);
  }

  public ServiceVersionInfo parseGradle(final String buildGradle) {
    return GradleBuildParser.parse(buildGradle);
  }

  private String readFile(final Path file) {
    try {
      return Files.readString(file);
    } catch (IOException e) {
      throw new AnalysisException("No se pudo leer el fichero " + file, e);
    }
  }
}
