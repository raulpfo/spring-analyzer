package io.github.springanalyzer.core.analyzer;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DependencyGraphBuilder {

  public DependencyGraph build(final List<ServiceSnapshot> services) {
    final List<DependencyEdge> edges = new ArrayList<>();
    final List<OrphanConsumption> orphanConsumptions = new ArrayList<>();

    for (final ServiceSnapshot consumer : services) {
      for (final EndpointConsumption consumption : consumer.consumptions()) {
        final List<DependencyEdge> matches = matchesOf(consumer, consumption, services);
        if (matches.isEmpty()) {
          orphanConsumptions.add(new OrphanConsumption(consumer.serviceName(), consumption));
        } else {
          edges.addAll(matches);
        }
      }
    }

    final Set<ConsumedEndpointKey> consumedEndpoints = new HashSet<>();
    for (final DependencyEdge edge : edges) {
      consumedEndpoints.add(new ConsumedEndpointKey(edge.producerService(), edge.endpoint()));
    }

    final List<ServiceNode> nodes = new ArrayList<>();
    final List<OrphanEndpoint> orphanEndpoints = new ArrayList<>();
    for (final ServiceSnapshot service : services) {
      nodes.add(new ServiceNode(service.serviceName(), service.versionInfo()));
      for (final Endpoint endpoint : service.endpoints()) {
        if (!consumedEndpoints.contains(new ConsumedEndpointKey(service.serviceName(), endpoint))) {
          orphanEndpoints.add(new OrphanEndpoint(service.serviceName(), endpoint));
        }
      }
    }

    return new DependencyGraph(nodes, edges, orphanEndpoints, orphanConsumptions);
  }

  private List<DependencyEdge> matchesOf(final ServiceSnapshot consumer, final EndpointConsumption consumption,
      final List<ServiceSnapshot> services) {
    final List<DependencyEdge> matches = new ArrayList<>();
    for (final ServiceSnapshot producer : services) {
      if (producer.serviceName().equals(consumer.serviceName())) {
        continue;
      }
      if (consumption.targetService() != null && !consumption.targetService().equals(producer.serviceName())) {
        continue;
      }
      for (final Endpoint endpoint : producer.endpoints()) {
        if (methodsMatch(endpoint.method(), consumption.method()) && pathsMatch(endpoint.path(), consumption.path())) {
          matches.add(new DependencyEdge(consumer.serviceName(), producer.serviceName(), consumption, endpoint));
        }
      }
    }
    return matches;
  }

  private static boolean methodsMatch(final HttpMethod endpointMethod, final HttpMethod consumptionMethod) {
    return endpointMethod == consumptionMethod
        || endpointMethod == HttpMethod.REQUEST
        || consumptionMethod == HttpMethod.REQUEST;
  }

  private static boolean pathsMatch(final String producerPath, final String consumerPath) {
    final String[] producerSegments = segmentsOf(producerPath);
    final String[] consumerSegments = segmentsOf(consumerPath);
    if (producerSegments.length != consumerSegments.length) {
      return false;
    }
    for (int i = 0; i < producerSegments.length; i++) {
      final String producerSegment = producerSegments[i];
      final String consumerSegment = consumerSegments[i];
      if (isVariable(producerSegment) || isVariable(consumerSegment)) {
        continue;
      }
      if (!producerSegment.equals(consumerSegment)) {
        return false;
      }
    }
    return true;
  }

  private static String[] segmentsOf(final String path) {
    return Arrays.stream(path.split("/")).filter(segment -> !segment.isEmpty()).toArray(String[]::new);
  }

  private static boolean isVariable(final String segment) {
    return segment.startsWith("{") && segment.endsWith("}");
  }

  private record ConsumedEndpointKey(String serviceName, Endpoint endpoint) {
  }
}
