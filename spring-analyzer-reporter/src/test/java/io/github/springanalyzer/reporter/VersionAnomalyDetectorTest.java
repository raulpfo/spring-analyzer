package io.github.springanalyzer.reporter;

import io.github.springanalyzer.core.analyzer.Dependency;
import io.github.springanalyzer.core.analyzer.ServiceNode;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VersionAnomalyDetectorTest {

  private final VersionAnomalyDetector detector = new VersionAnomalyDetector();

  @Test
  void flagsTheServiceWithTheOlderSpringBootVersion() {
    final ServiceNode upToDate = node("order-service", "3.4.0", List.of());
    final ServiceNode behind = node("user-service", "2.7.5", List.of());

    final List<OutdatedVersion> anomalies = detector.detect(List.of(upToDate, behind));

    assertThat(anomalies).containsExactly(new OutdatedVersion("user-service", "Spring Boot", "2.7.5", "3.4.0"));
  }

  @Test
  void doesNotFlagServicesSharingTheSameSpringBootVersion() {
    final ServiceNode first = node("order-service", "3.4.0", List.of());
    final ServiceNode second = node("user-service", "3.4.0", List.of());

    assertThat(detector.detect(List.of(first, second))).isEmpty();
  }

  @Test
  void doesNotFlagASingleServiceWithNoPeersToCompareAgainst() {
    final ServiceNode onlyService = node("order-service", "2.7.5", List.of());

    assertThat(detector.detect(List.of(onlyService))).isEmpty();
  }

  @Test
  void ignoresServicesWithUnknownSpringBootVersionWhenComparing() {
    final ServiceNode known = node("order-service", "3.4.0", List.of());
    final ServiceNode unknown = new ServiceNode("user-service", ServiceVersionInfo.unknown());

    assertThat(detector.detect(List.of(known, unknown))).isEmpty();
  }

  @Test
  void comparesVersionSegmentsNumericallyRatherThanLexicographically() {
    final ServiceNode newer = node("order-service", "2.7.10", List.of());
    final ServiceNode older = node("user-service", "2.7.5", List.of());

    final List<OutdatedVersion> anomalies = detector.detect(List.of(newer, older));

    assertThat(anomalies).containsExactly(new OutdatedVersion("user-service", "Spring Boot", "2.7.5", "2.7.10"));
  }

  @Test
  void flagsAServiceWithAnOutdatedDependencyVersionIndependentlyPerArtifact() {
    final ServiceNode upToDate = node("order-service", null,
        List.of(new Dependency("org.apache.commons", "commons-lang3", "3.14.0")));
    final ServiceNode behind = node("user-service", null,
        List.of(new Dependency("org.apache.commons", "commons-lang3", "3.9")));

    final List<OutdatedVersion> anomalies = detector.detect(List.of(upToDate, behind));

    assertThat(anomalies).containsExactly(
        new OutdatedVersion("user-service", "org.apache.commons:commons-lang3", "3.9", "3.14.0"));
  }

  @Test
  void skipsDependenciesWithoutAKnownVersionWhenComparing() {
    final ServiceNode withVersion = node("order-service", null,
        List.of(new Dependency("org.apache.commons", "commons-lang3", "3.14.0")));
    final ServiceNode withoutVersion = node("user-service", null,
        List.of(new Dependency("org.apache.commons", "commons-lang3", null)));

    assertThat(detector.detect(List.of(withVersion, withoutVersion))).isEmpty();
  }

  private static ServiceNode node(final String serviceName, final String springBootVersion,
      final List<Dependency> dependencies) {
    return new ServiceNode(serviceName, new ServiceVersionInfo(springBootVersion, null, dependencies));
  }
}
