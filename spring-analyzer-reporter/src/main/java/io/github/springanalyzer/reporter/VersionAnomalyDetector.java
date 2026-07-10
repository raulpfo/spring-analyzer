package io.github.springanalyzer.reporter;

import io.github.springanalyzer.core.analyzer.Dependency;
import io.github.springanalyzer.core.analyzer.ServiceNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VersionAnomalyDetector {

  public List<OutdatedVersion> detect(final List<ServiceNode> nodes) {
    final List<OutdatedVersion> anomalies = new ArrayList<>();
    anomalies.addAll(anomaliesFor("Spring Boot", springBootVersionsByService(nodes)));
    for (final Map.Entry<String, Map<String, String>> artifact : dependencyVersionsByArtifact(nodes).entrySet()) {
      anomalies.addAll(anomaliesFor(artifact.getKey(), artifact.getValue()));
    }
    return anomalies;
  }

  private static Map<String, String> springBootVersionsByService(final List<ServiceNode> nodes) {
    final Map<String, String> versionsByService = new LinkedHashMap<>();
    for (final ServiceNode node : nodes) {
      if (node.versionInfo().springBootVersion() != null) {
        versionsByService.put(node.serviceName(), node.versionInfo().springBootVersion());
      }
    }
    return versionsByService;
  }

  private static Map<String, Map<String, String>> dependencyVersionsByArtifact(final List<ServiceNode> nodes) {
    final Map<String, Map<String, String>> versionsByArtifact = new LinkedHashMap<>();
    for (final ServiceNode node : nodes) {
      for (final Dependency dependency : node.versionInfo().dependencies()) {
        if (dependency.version() == null) {
          continue;
        }
        final String artifactKey = dependency.groupId() + ":" + dependency.artifactId();
        versionsByArtifact.computeIfAbsent(artifactKey, key -> new LinkedHashMap<>())
            .put(node.serviceName(), dependency.version());
      }
    }
    return versionsByArtifact;
  }

  private static List<OutdatedVersion> anomaliesFor(final String label, final Map<String, String> versionsByService) {
    final String latestVersion = versionsByService.values().stream().max(VersionAnomalyDetector::compareVersions)
        .orElse(null);
    final List<OutdatedVersion> anomalies = new ArrayList<>();
    for (final Map.Entry<String, String> entry : versionsByService.entrySet()) {
      if (compareVersions(entry.getValue(), latestVersion) < 0) {
        anomalies.add(new OutdatedVersion(entry.getKey(), label, entry.getValue(), latestVersion));
      }
    }
    return anomalies;
  }

  static int compareVersions(final String versionA, final String versionB) {
    final String[] partsA = versionA.split("[.\\-_]");
    final String[] partsB = versionB.split("[.\\-_]");
    final int length = Math.max(partsA.length, partsB.length);
    for (int i = 0; i < length; i++) {
      final String partA = i < partsA.length ? partsA[i] : "";
      final String partB = i < partsB.length ? partsB[i] : "";
      final int comparison = comparePart(partA, partB);
      if (comparison != 0) {
        return comparison;
      }
    }
    return 0;
  }

  private static int comparePart(final String partA, final String partB) {
    if (isNumeric(partA) && isNumeric(partB)) {
      return Integer.compare(Integer.parseInt(partA), Integer.parseInt(partB));
    }
    if (partA.isEmpty() != partB.isEmpty()) {
      return partA.isEmpty() ? -1 : 1;
    }
    return partA.compareTo(partB);
  }

  private static boolean isNumeric(final String part) {
    return !part.isEmpty() && part.chars().allMatch(Character::isDigit);
  }
}
