package io.github.springanalyzer.analyzers.version;

import io.github.springanalyzer.core.analyzer.Dependency;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GradleBuildParser {

  private static final Pattern SPRING_BOOT_PLUGIN =
      Pattern.compile("id\\s+['\"]org\\.springframework\\.boot['\"]\\s+version\\s+['\"]([^'\"]+)['\"]");

  private static final Pattern JAVA_VERSION =
      Pattern.compile("sourceCompatibility\\s*=\\s*['\"]?(?:JavaVersion\\.VERSION_)?([\\w.]+)['\"]?");

  private static final Pattern DEPENDENCY_DECLARATION = Pattern.compile(
      "(?:implementation|api|compile|runtimeOnly|testImplementation|annotationProcessor)"
          + "\\s*\\(?\\s*['\"]([\\w.\\-]+):([\\w.\\-]+)(?::([\\w.\\-]+))?['\"]");

  private GradleBuildParser() {
  }

  static ServiceVersionInfo parse(final String buildGradle) {
    return new ServiceVersionInfo(firstGroupMatch(SPRING_BOOT_PLUGIN, buildGradle).orElse(null),
        firstGroupMatch(JAVA_VERSION, buildGradle).orElse(null), dependenciesOf(buildGradle));
  }

  private static Optional<String> firstGroupMatch(final Pattern pattern, final String text) {
    final Matcher matcher = pattern.matcher(text);
    return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
  }

  private static List<Dependency> dependenciesOf(final String buildGradle) {
    final Matcher matcher = DEPENDENCY_DECLARATION.matcher(buildGradle);
    final List<Dependency> dependencies = new ArrayList<>();
    while (matcher.find()) {
      dependencies.add(new Dependency(matcher.group(1), matcher.group(2), matcher.group(3)));
    }
    return dependencies;
  }
}
