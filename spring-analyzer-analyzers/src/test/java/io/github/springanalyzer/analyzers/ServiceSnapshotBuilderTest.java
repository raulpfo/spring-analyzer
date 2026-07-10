package io.github.springanalyzer.analyzers;

import io.github.springanalyzer.analyzers.spring.SpringConsumerAnalyzer;
import io.github.springanalyzer.analyzers.spring.SpringJavaEndpointAnalyzer;
import io.github.springanalyzer.analyzers.version.ServiceVersionAnalyzer;
import io.github.springanalyzer.core.analyzer.Endpoint;
import io.github.springanalyzer.core.analyzer.EndpointConsumption;
import io.github.springanalyzer.core.analyzer.HttpMethod;
import io.github.springanalyzer.core.analyzer.RepoContext;
import io.github.springanalyzer.core.analyzer.ServiceSnapshot;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceSnapshotBuilderTest {

  private SpringJavaEndpointAnalyzer endpointAnalyzer;
  private SpringConsumerAnalyzer consumerAnalyzer;
  private ServiceVersionAnalyzer versionAnalyzer;
  private ServiceSnapshotBuilder builder;

  @BeforeEach
  void setUp() {
    endpointAnalyzer = mock(SpringJavaEndpointAnalyzer.class);
    consumerAnalyzer = mock(SpringConsumerAnalyzer.class);
    versionAnalyzer = mock(ServiceVersionAnalyzer.class);
    builder = new ServiceSnapshotBuilder(endpointAnalyzer, consumerAnalyzer, versionAnalyzer);
  }

  @Test
  void combinesEndpointsConsumptionsAndVersionInfoIntoASnapshot(@TempDir final Path repoDir) {
    final List<Endpoint> endpoints = List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController"));
    final List<EndpointConsumption> consumptions = List.of(new EndpointConsumption("user-service", "/users/{id}", HttpMethod.GET));
    final ServiceVersionInfo versionInfo = new ServiceVersionInfo("3.4.0", "21", List.of());
    when(endpointAnalyzer.analyze(repoDir)).thenReturn(endpoints);
    when(consumerAnalyzer.analyze(repoDir)).thenReturn(consumptions);
    when(versionAnalyzer.analyze(repoDir)).thenReturn(versionInfo);

    final ServiceSnapshot snapshot = builder.build(new RepoContext("order-service", repoDir));

    assertThat(snapshot).isEqualTo(new ServiceSnapshot("order-service", endpoints, consumptions, versionInfo));
  }
}
