package io.github.springanalyzer.boot;

import io.github.springanalyzer.boot.support.FixtureGitRepo;
import io.github.springanalyzer.commands.AnalyzeCommand;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the full flow (CLI command -> clone -> analysis -> graph -> HTML report) against local
 * fixture repositories, with no network access and no real credentials.
 *
 * <p>The fixture repos.yml sets {@code provider: github} explicitly for every entry: the fixture
 * URLs are {@code file://} paths, and {@link io.github.springanalyzer.domain.entities.ScmProvider}
 * can only infer a provider from a host name, so it cannot be auto-detected for a local path.
 *
 * <p>The {@code CredentialResolver} used inside the pipeline still falls back to the real
 * {@code GITHUB_TOKEN} environment variable if it happens to be set (e.g. on a CI runner), since
 * no {@code --github-token} flag is passed here. This is harmless: JGit's local file transport
 * never invokes the credentials provider, so any such token is never read or transmitted.
 *
 * <p>Boots the context from {@link TestBootstrap} instead of {@link SpringAnalyzerApplication}
 * to avoid triggering the latter's {@code CommandLineRunner}: @SpringBootTest invokes registered
 * CommandLineRunner beans just like a real startup would, and that one would run picocli against
 * the test process's own arguments (failing to parse them and calling System.exit).
 * {@link SpringAnalyzerApplication} is explicitly excluded from component scanning so its
 * {@code commandLineRunner} bean is never registered in the test context.
 */
@SpringBootTest(classes = EndToEndAnalysisTest.TestBootstrap.class)
class EndToEndAnalysisTest {

  @Configuration
  @EnableAutoConfiguration
  @ComponentScan(basePackages = "io.github.springanalyzer",
      excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SpringAnalyzerApplication.class))
  static class TestBootstrap {
  }

  private static final String ORDER_SERVICE_POM = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-parent</artifactId>
          <version>3.4.0</version>
        </parent>
        <groupId>com.example</groupId>
        <artifactId>order-service</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <properties>
          <java.version>21</java.version>
        </properties>
        <dependencies>
          <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
          <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.14.0</version>
          </dependency>
        </dependencies>
      </project>
      """;

  private static final String ORDER_CONTROLLER = """
      package com.example.orderservice;

      import org.springframework.web.bind.annotation.GetMapping;
      import org.springframework.web.bind.annotation.PathVariable;
      import org.springframework.web.bind.annotation.RestController;

      @RestController
      public class OrderController {

        @GetMapping("/orders/{id}")
        public String getOrder(@PathVariable String id) {
          return "order-" + id;
        }
      }
      """;

  private static final String ORDER_SERVICE_USER_CLIENT = """
      package com.example.orderservice;

      import org.springframework.cloud.openfeign.FeignClient;
      import org.springframework.web.bind.annotation.GetMapping;
      import org.springframework.web.bind.annotation.PathVariable;

      @FeignClient(name = "user-service")
      public interface UserClient {

        @GetMapping("/users/{id}")
        String getUser(@PathVariable String id);
      }
      """;

  private static final String USER_SERVICE_POM = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-parent</artifactId>
          <version>2.7.5</version>
        </parent>
        <groupId>com.example</groupId>
        <artifactId>user-service</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <properties>
          <java.version>17</java.version>
        </properties>
        <dependencies>
          <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
          <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
            <version>3.9</version>
          </dependency>
        </dependencies>
      </project>
      """;

  private static final String USER_CONTROLLER = """
      package com.example.userservice;

      import org.springframework.web.bind.annotation.GetMapping;
      import org.springframework.web.bind.annotation.PathVariable;
      import org.springframework.web.bind.annotation.RestController;

      @RestController
      public class UserController {

        @GetMapping("/users/{id}")
        public String getUser(@PathVariable String id) {
          return "user-" + id;
        }

        @GetMapping("/users/{id}/preferences")
        public String getPreferences(@PathVariable String id) {
          return "preferences-" + id;
        }
      }
      """;

  private static final String USER_SERVICE_ORDER_CLIENT = """
      package com.example.userservice;

      import org.springframework.cloud.openfeign.FeignClient;
      import org.springframework.web.bind.annotation.GetMapping;
      import org.springframework.web.bind.annotation.PathVariable;

      @FeignClient(name = "order-service")
      public interface OrderClient {

        @GetMapping("/orders/{id}")
        String getOrder(@PathVariable String id);
      }
      """;

  private static final String USER_SERVICE_INVOICE_CLIENT = """
      package com.example.userservice;

      import org.springframework.cloud.openfeign.FeignClient;
      import org.springframework.web.bind.annotation.GetMapping;
      import org.springframework.web.bind.annotation.PathVariable;

      @FeignClient(name = "billing-service")
      public interface InvoiceClient {

        @GetMapping("/invoices/{id}")
        String getInvoice(@PathVariable String id);
      }
      """;

  private static final String BILLING_SERVICE_POM = """
      <?xml version="1.0" encoding="UTF-8"?>
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-starter-parent</artifactId>
          <version>3.4.0</version>
        </parent>
        <groupId>com.example</groupId>
        <artifactId>billing-service</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <properties>
          <java.version>21</java.version>
        </properties>
        <dependencies>
          <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
          </dependency>
        </dependencies>
      </project>
      """;

  private static final String BILLING_APPLICATION = """
      package com.example.billingservice;

      public class BillingApplication {
        public static void main(String[] args) {
        }
      }
      """;

  @Autowired
  private AnalyzeCommand analyzeCommand;

  @Autowired
  private CommandLine.IFactory commandFactory;

  @Test
  void analyzesFixtureMicroservicesEndToEndAndProducesAnAccurateHtmlReport(@TempDir final Path tempDir)
      throws IOException {
    final Path reposDir = tempDir.resolve("repos");
    final String orderServiceUrl = FixtureGitRepo.create(reposDir, "order-service", Map.of(
        "pom.xml", ORDER_SERVICE_POM,
        "src/main/java/com/example/orderservice/OrderController.java", ORDER_CONTROLLER,
        "src/main/java/com/example/orderservice/UserClient.java", ORDER_SERVICE_USER_CLIENT));
    final String userServiceUrl = FixtureGitRepo.create(reposDir, "user-service", Map.of(
        "pom.xml", USER_SERVICE_POM,
        "src/main/java/com/example/userservice/UserController.java", USER_CONTROLLER,
        "src/main/java/com/example/userservice/OrderClient.java", USER_SERVICE_ORDER_CLIENT,
        "src/main/java/com/example/userservice/InvoiceClient.java", USER_SERVICE_INVOICE_CLIENT));
    final String billingServiceUrl = FixtureGitRepo.create(reposDir, "billing-service", Map.of(
        "pom.xml", BILLING_SERVICE_POM,
        "src/main/java/com/example/billingservice/BillingApplication.java", BILLING_APPLICATION));

    final Path reposYml = tempDir.resolve("repos.yml");
    Files.writeString(reposYml, """
        repos:
          - url: %s
            provider: github
          - url: %s
            provider: github
          - url: %s
            provider: github
        """.formatted(orderServiceUrl, userServiceUrl, billingServiceUrl));

    final Path reportPath = tempDir.resolve("report.html");

    final int exitCode = new CommandLine(analyzeCommand, commandFactory)
        .execute("-c", reposYml.toString(), "-o", reportPath.toString(), "--threads", "2");

    assertThat(exitCode).isZero();
    assertThat(reportPath).exists();

    final Document report = Jsoup.parse(reportPath.toFile(), "UTF-8");

    assertVersionsTable(report);
    assertDependencyGraph(report);
    assertOrphanEndpoints(report);
    assertOrphanConsumptions(report);
    assertOutdatedVersions(report);
  }

  private void assertVersionsTable(final Document report) {
    final Elements rows = report.select("#versions-table tbody tr");
    assertThat(rows).hasSize(3);
    assertThat(rowFor(rows, "order-service").select("td").get(1).text()).isEqualTo("3.4.0");
    assertThat(rowFor(rows, "user-service").select("td").get(1).text()).isEqualTo("2.7.5");
    assertThat(rowFor(rows, "billing-service").select("td").get(1).text()).isEqualTo("3.4.0");
  }

  private void assertDependencyGraph(final Document report) {
    final String mermaid = report.select("pre.mermaid").text();
    assertThat(mermaid).contains("order_service").contains("user_service").contains("billing_service");
    assertThat(mermaid).contains("order_service -->|\"GET /users/{id}\"| user_service");
    assertThat(mermaid).contains("user_service -->|\"GET /orders/{id}\"| order_service");
  }

  private void assertOrphanEndpoints(final Document report) {
    final Elements rows = report.select("#orphan-endpoints-table tbody tr");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).select("td").get(0).text()).isEqualTo("user-service");
    assertThat(rows.get(0).select("td").get(2).text()).isEqualTo("/users/{id}/preferences");
  }

  private void assertOrphanConsumptions(final Document report) {
    final Elements rows = report.select("#orphan-consumptions-table tbody tr");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).select("td").get(0).text()).isEqualTo("user-service");
    assertThat(rows.get(0).select("td").get(1).text()).isEqualTo("billing-service");
    assertThat(rows.get(0).select("td").get(3).text()).isEqualTo("/invoices/{id}");
  }

  private void assertOutdatedVersions(final Document report) {
    final Elements rows = report.select("#outdated-versions-table tbody tr");
    assertThat(rows).extracting(row -> row.select("td").get(0).text()).containsOnly("user-service");
    assertThat(rows).extracting(row -> row.select("td").get(1).text())
        .containsExactlyInAnyOrder("Spring Boot", "org.apache.commons:commons-lang3");
  }

  private static Element rowFor(final Elements rows, final String serviceName) {
    return rows.stream().filter(row -> row.select("td").get(0).text().equals(serviceName)).findFirst()
        .orElseThrow(() -> new AssertionError("No row found for service " + serviceName));
  }
}
