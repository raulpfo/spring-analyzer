package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DependencyGraphBuilderTest {

  private final DependencyGraphBuilder builder = new DependencyGraphBuilder();

  @Test
  void linksConsumerToProducerWhenTargetServicePathAndMethodMatch() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders/{id}", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.edges()).extracting(DependencyEdge::consumerService, DependencyEdge::producerService)
        .containsExactly(tuple("user-service", "order-service"));
    assertThat(graph.orphanEndpoints()).isEmpty();
    assertThat(graph.orphanConsumptions()).isEmpty();
  }

  @Test
  void matchesPathVariablesRegardlessOfTheirName() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders/{orderId}", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.edges()).hasSize(1);
  }

  @Test
  void matchesPathVariableAgainstAConcreteLiteralSegment() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders/42", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.edges()).hasSize(1);
  }

  @Test
  void doesNotMatchPathsWithDifferentSegmentCounts() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders/{id}/items", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.edges()).isEmpty();
    assertThat(graph.orphanConsumptions()).hasSize(1);
  }

  @Test
  void aRequestMappingEndpointMatchesConsumptionsOfAnyHttpMethod() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.REQUEST, "/orders", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders", HttpMethod.POST)));

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.edges()).hasSize(1);
  }

  @Test
  void doesNotMatchDifferentHttpMethods() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders", HttpMethod.POST)));

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.edges()).isEmpty();
    assertThat(graph.orphanConsumptions()).hasSize(1);
  }

  @Test
  void resolvesConsumptionWithUnknownTargetServiceBySearchingAllOtherServices() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption(null, "/orders/{id}", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.edges()).extracting(DependencyEdge::producerService).containsExactly("order-service");
    assertThat(graph.orphanConsumptions()).isEmpty();
  }

  @Test
  void linksConsumptionWithUnknownTargetServiceToEveryMatchingProducerWhenSeveralMatch() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot legacyOrderService = service("legacy-order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.LegacyOrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption(null, "/orders/{id}", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService, legacyOrderService, userService));

    assertThat(graph.edges()).extracting(DependencyEdge::producerService)
        .containsExactlyInAnyOrder("order-service", "legacy-order-service");
    assertThat(graph.orphanConsumptions()).isEmpty();
  }

  @Test
  void ignoresEndpointsOfTheSameServiceWhenResolvingItsOwnConsumptions() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")),
        List.of(new EndpointConsumption(null, "/orders/{id}", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService));

    assertThat(graph.edges()).isEmpty();
    assertThat(graph.orphanConsumptions()).hasSize(1);
    assertThat(graph.orphanEndpoints()).hasSize(1);
  }

  @Test
  void doesNotMatchConsumptionAgainstAServiceOtherThanItsExplicitTargetEvenIfThePathAndMethodMatch() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot billingService = service("billing-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.BillingOrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders/{id}", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService, billingService, userService));

    assertThat(graph.edges()).extracting(DependencyEdge::producerService).containsExactly("order-service");
  }

  @Test
  void marksEndpointAsOrphanWhenNoConsumptionMatchesIt() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(), List.of());

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.orphanEndpoints()).extracting(OrphanEndpoint::serviceName)
        .containsExactly("order-service");
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  void marksConsumptionAsOrphanWhenNoEndpointMatchesIt() {
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders/{id}", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(userService));

    assertThat(graph.orphanConsumptions()).extracting(OrphanConsumption::serviceName)
        .containsExactly("user-service");
    assertThat(graph.edges()).isEmpty();
  }

  @Test
  void doesNotConfuseOrphanEndpointsAcrossServicesWithStructurallyIdenticalEndpoints() {
    final Endpoint sameShapeEndpoint = new Endpoint(HttpMethod.GET, "/health", "com.example.HealthController");
    final ServiceSnapshot orderService = service("order-service", List.of(sameShapeEndpoint), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(sameShapeEndpoint),
        List.of(new EndpointConsumption("order-service", "/health", HttpMethod.GET)));

    final DependencyGraph graph = builder.build(List.of(orderService, userService));

    assertThat(graph.edges()).extracting(DependencyEdge::producerService).containsExactly("order-service");
    assertThat(graph.orphanEndpoints()).extracting(OrphanEndpoint::serviceName).containsExactly("user-service");
  }

  @Test
  void createsANodePerServiceCarryingItsVersionInfo() {
    final ServiceVersionInfo versionInfo = new ServiceVersionInfo("3.4.0", "21", List.of());
    final ServiceSnapshot orderService = new ServiceSnapshot("order-service", List.of(), List.of(), versionInfo);

    final DependencyGraph graph = builder.build(List.of(orderService));

    assertThat(graph.nodes()).containsExactly(new ServiceNode("order-service", versionInfo));
  }

  @Test
  void producesTheIdenticalResultOnRepeatedCallsForTheSameServiceList() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders/{id}", HttpMethod.GET)));

    final DependencyGraph first = builder.build(List.of(orderService, userService));
    final DependencyGraph second = builder.build(List.of(orderService, userService));

    assertThat(first).isEqualTo(second);
  }

  @Test
  void producesTheSameEdgesAndOrphansRegardlessOfServiceOrderInTheInput() {
    final ServiceSnapshot orderService = service("order-service",
        List.of(new Endpoint(HttpMethod.GET, "/orders/{id}", "com.example.OrderController")), List.of());
    final ServiceSnapshot userService = service("user-service", List.of(),
        List.of(new EndpointConsumption("order-service", "/orders/{id}", HttpMethod.GET)));

    final DependencyGraph inOriginalOrder = builder.build(List.of(orderService, userService));
    final DependencyGraph inReversedOrder = builder.build(List.of(userService, orderService));

    assertThat(inReversedOrder.edges()).containsExactlyInAnyOrderElementsOf(inOriginalOrder.edges());
    assertThat(inReversedOrder.nodes()).containsExactlyInAnyOrderElementsOf(inOriginalOrder.nodes());
    assertThat(inReversedOrder.orphanEndpoints()).containsExactlyInAnyOrderElementsOf(inOriginalOrder.orphanEndpoints());
    assertThat(inReversedOrder.orphanConsumptions())
        .containsExactlyInAnyOrderElementsOf(inOriginalOrder.orphanConsumptions());
  }

  @Test
  void returnsAnEmptyGraphWhenThereAreNoServices() {
    final DependencyGraph graph = builder.build(List.of());

    assertThat(graph.nodes()).isEmpty();
    assertThat(graph.edges()).isEmpty();
    assertThat(graph.orphanEndpoints()).isEmpty();
    assertThat(graph.orphanConsumptions()).isEmpty();
  }

  private static ServiceSnapshot service(final String name, final List<Endpoint> endpoints,
      final List<EndpointConsumption> consumptions) {
    return new ServiceSnapshot(name, endpoints, consumptions, ServiceVersionInfo.unknown());
  }
}
