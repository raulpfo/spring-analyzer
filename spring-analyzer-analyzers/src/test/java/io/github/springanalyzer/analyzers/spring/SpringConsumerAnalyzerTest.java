package io.github.springanalyzer.analyzers.spring;

import io.github.springanalyzer.core.analyzer.EndpointConsumption;
import io.github.springanalyzer.core.analyzer.HttpMethod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SpringConsumerAnalyzerTest {

  private final SpringConsumerAnalyzer analyzer = new SpringConsumerAnalyzer();

  @Test
  void detectsFeignClientEndpointsCombiningClassAndMethodPath() {
    final String source = """
        package com.example;

        import org.springframework.cloud.openfeign.FeignClient;
        import org.springframework.web.bind.annotation.*;

        @FeignClient(name = "order-service")
        @RequestMapping("/api/orders")
        public interface OrderClient {

          @GetMapping("/{id}")
          String get();

          @PostMapping
          String create();
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).extracting(EndpointConsumption::targetService, EndpointConsumption::path,
            EndpointConsumption::method)
        .containsExactlyInAnyOrder(
            tuple("order-service", "/api/orders/{id}", HttpMethod.GET),
            tuple("order-service", "/api/orders", HttpMethod.POST));
  }

  @Test
  void detectsFeignClientUsingValueAttribute() {
    final String source = """
        package com.example;

        import org.springframework.cloud.openfeign.FeignClient;
        import org.springframework.web.bind.annotation.*;

        @FeignClient("order-service")
        public interface OrderClient {

          @GetMapping("/orders")
          String list();
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).containsExactly(new EndpointConsumption("order-service", "/orders", HttpMethod.GET));
  }

  @Test
  void marksTargetServiceAsUnknownWhenFeignClientHasNoNameOrValue() {
    final String source = """
        package com.example;

        import org.springframework.cloud.openfeign.FeignClient;
        import org.springframework.web.bind.annotation.*;

        @FeignClient
        public interface OrderClient {

          @GetMapping("/orders")
          String list();
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).containsExactly(new EndpointConsumption(null, "/orders", HttpMethod.GET));
  }

  @Test
  void detectsRestTemplateCallsInferringTargetServiceFromAbsoluteUrl() {
    final String source = """
        package com.example;

        import org.springframework.web.client.RestTemplate;

        public class OrderGateway {

          private final RestTemplate restTemplate;

          public OrderGateway(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
          }

          public String fetch(String id) {
            return restTemplate.getForObject("http://order-service/orders/{id}", String.class, id);
          }
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions)
        .containsExactly(new EndpointConsumption("order-service", "/orders/{id}", HttpMethod.GET));
  }

  @Test
  void marksTargetServiceAsUnknownWhenRestTemplateUrlIsRelative() {
    final String source = """
        package com.example;

        import org.springframework.web.client.RestTemplate;

        public class OrderGateway {

          private final RestTemplate restTemplate;

          public OrderGateway(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
          }

          public void create(String payload) {
            restTemplate.postForObject("/orders", payload, String.class);
          }
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).containsExactly(new EndpointConsumption(null, "/orders", HttpMethod.POST));
  }

  @Test
  void detectsRestTemplatePutAndDelete() {
    final String source = """
        package com.example;

        import org.springframework.web.client.RestTemplate;

        public class OrderGateway {

          private final RestTemplate restTemplate;

          public OrderGateway(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
          }

          public void update(String payload) {
            restTemplate.put("http://order-service/orders", payload);
          }

          public void remove() {
            restTemplate.delete("http://order-service/orders/{id}");
          }
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).extracting(EndpointConsumption::targetService, EndpointConsumption::path,
            EndpointConsumption::method)
        .containsExactlyInAnyOrder(
            tuple("order-service", "/orders", HttpMethod.PUT),
            tuple("order-service", "/orders/{id}", HttpMethod.DELETE));
  }

  @Test
  void detectsRestTemplateExchangeWithLiteralHttpMethodAndSkipsUnresolvableOnes() {
    final String source = """
        package com.example;

        import org.springframework.http.HttpMethod;
        import org.springframework.web.client.RestTemplate;

        public class OrderGateway {

          private final RestTemplate restTemplate;

          public OrderGateway(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
          }

          public void known(HttpMethod dynamicMethod) {
            restTemplate.exchange("http://order-service/orders", HttpMethod.POST, null, String.class);
            restTemplate.exchange("http://order-service/orders", dynamicMethod, null, String.class);
          }
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).containsExactly(new EndpointConsumption("order-service", "/orders", HttpMethod.POST));
  }

  @Test
  void detectsWebClientFluentChainWithAbsoluteUrl() {
    final String source = """
        package com.example;

        import org.springframework.web.reactive.function.client.WebClient;

        public class OrderGateway {

          private final WebClient webClient;

          public OrderGateway(WebClient webClient) {
            this.webClient = webClient;
          }

          public void create(String payload) {
            webClient.post().uri("http://order-service/orders").bodyValue(payload).retrieve();
          }
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions)
        .containsExactly(new EndpointConsumption("order-service", "/orders", HttpMethod.POST));
  }

  @Test
  void marksTargetServiceAsUnknownWhenWebClientUriIsRelative() {
    final String source = """
        package com.example;

        import org.springframework.web.reactive.function.client.WebClient;

        public class OrderGateway {

          private final WebClient webClient;

          public OrderGateway(WebClient webClient) {
            this.webClient = webClient;
          }

          public void fetch() {
            webClient.get().uri("/orders").retrieve();
          }
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).containsExactly(new EndpointConsumption(null, "/orders", HttpMethod.GET));
  }

  @Test
  void resolvesInterpolatedUrlKeepingKnownLiteralSegmentsAndMarkingUnknownPartsWithAPlaceholder() {
    final String source = """
        package com.example;

        import org.springframework.web.client.RestTemplate;

        public class OrderGateway {

          private final RestTemplate restTemplate;

          public OrderGateway(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
          }

          public String fetch(String serviceName, String id) {
            return restTemplate.getForObject("http://" + serviceName + "/orders/" + id, String.class);
          }
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).containsExactly(new EndpointConsumption(null, "/orders/{}", HttpMethod.GET));
  }

  @Test
  void ignoresCallsOnVariablesThatAreNotRecognizedHttpClients() {
    final String source = """
        package com.example;

        public class OrderGateway {

          private final OrderRepository orderRepository;

          public OrderGateway(OrderRepository orderRepository) {
            this.orderRepository = orderRepository;
          }

          public void save() {
            orderRepository.delete("/orders");
          }
        }
        """;

    final List<EndpointConsumption> consumptions = analyzer.analyzeSource(source);

    assertThat(consumptions).isEmpty();
  }

  @Test
  void analyzesAllJavaFilesUnderADirectoryRecursively(@TempDir final Path repoRoot) throws IOException {
    final Path packageDir = repoRoot.resolve("src/main/java/com/example");
    Files.createDirectories(packageDir);
    Files.writeString(packageDir.resolve("OrderClient.java"), """
        package com.example;

        import org.springframework.cloud.openfeign.FeignClient;
        import org.springframework.web.bind.annotation.*;

        @FeignClient(name = "order-service")
        public interface OrderClient {

          @GetMapping("/orders")
          String list();
        }
        """);
    Files.writeString(packageDir.resolve("PaymentGateway.java"), """
        package com.example;

        import org.springframework.web.client.RestTemplate;

        public class PaymentGateway {

          private final RestTemplate restTemplate;

          public PaymentGateway(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
          }

          public void pay() {
            restTemplate.postForObject("http://payment-service/payments", null, String.class);
          }
        }
        """);

    final List<EndpointConsumption> consumptions = analyzer.analyze(repoRoot);

    assertThat(consumptions).extracting(EndpointConsumption::targetService, EndpointConsumption::path,
            EndpointConsumption::method)
        .containsExactlyInAnyOrder(
            tuple("order-service", "/orders", HttpMethod.GET),
            tuple("payment-service", "/payments", HttpMethod.POST));
  }
}
