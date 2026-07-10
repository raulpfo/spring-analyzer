package io.github.springanalyzer.analyzers;

import io.github.springanalyzer.analyzers.spring.SpringConsumerAnalyzer;
import io.github.springanalyzer.analyzers.spring.SpringJavaEndpointAnalyzer;
import io.github.springanalyzer.analyzers.version.ServiceVersionAnalyzer;
import io.github.springanalyzer.core.analyzer.Endpoint;
import io.github.springanalyzer.core.analyzer.EndpointConsumption;
import io.github.springanalyzer.core.analyzer.RepoContext;
import io.github.springanalyzer.core.analyzer.ServiceSnapshot;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ServiceSnapshotBuilder {

  private final SpringJavaEndpointAnalyzer endpointAnalyzer;
  private final SpringConsumerAnalyzer consumerAnalyzer;
  private final ServiceVersionAnalyzer versionAnalyzer;

  public ServiceSnapshotBuilder(final SpringJavaEndpointAnalyzer endpointAnalyzer,
      final SpringConsumerAnalyzer consumerAnalyzer, final ServiceVersionAnalyzer versionAnalyzer) {
    this.endpointAnalyzer = endpointAnalyzer;
    this.consumerAnalyzer = consumerAnalyzer;
    this.versionAnalyzer = versionAnalyzer;
  }

  public ServiceSnapshot build(final RepoContext context) {
    final List<Endpoint> endpoints = endpointAnalyzer.analyze(context.localPath());
    final List<EndpointConsumption> consumptions = consumerAnalyzer.analyze(context.localPath());
    final ServiceVersionInfo versionInfo = versionAnalyzer.analyze(context.localPath());
    return new ServiceSnapshot(context.repoName(), endpoints, consumptions, versionInfo);
  }
}
