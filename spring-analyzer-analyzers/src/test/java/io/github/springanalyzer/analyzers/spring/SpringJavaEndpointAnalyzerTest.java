package io.github.springanalyzer.analyzers.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SpringJavaEndpointAnalyzerTest {

  private final SpringJavaEndpointAnalyzer analyzer = new SpringJavaEndpointAnalyzer();

  @Test
  void combinesClassAndMethodPathsForEachMappingAnnotation() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        @RequestMapping("/api/users")
        public class UserController {

          @GetMapping("/{id}")
          public String get() { return ""; }

          @PostMapping("/")
          public String create() { return ""; }

          @PutMapping("{id}")
          public String update() { return ""; }

          @DeleteMapping("/{id}")
          public String delete() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).extracting(Endpoint::method, Endpoint::path, Endpoint::owner)
        .containsExactlyInAnyOrder(
            tuple(HttpMethod.GET, "/api/users/{id}", "com.example.UserController"),
            tuple(HttpMethod.POST, "/api/users", "com.example.UserController"),
            tuple(HttpMethod.PUT, "/api/users/{id}", "com.example.UserController"),
            tuple(HttpMethod.DELETE, "/api/users/{id}", "com.example.UserController"));
  }

  @Test
  void usesClassPathWhenMethodMappingHasNoPath() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        @RequestMapping("/api/users")
        public class UserController {

          @GetMapping
          public String list() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).containsExactly(new Endpoint(HttpMethod.GET, "/api/users", "com.example.UserController"));
  }

  @Test
  void treatsEmptyStringPathAsNoPath() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        @RequestMapping("/api/users")
        public class UserController {

          @GetMapping("")
          public String list() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).containsExactly(new Endpoint(HttpMethod.GET, "/api/users", "com.example.UserController"));
  }

  @Test
  void defaultsToRootPathWhenNeitherClassNorMethodDeclareAPath() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        public class RootController {

          @GetMapping
          public String root() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).containsExactly(new Endpoint(HttpMethod.GET, "/", "com.example.RootController"));
  }

  @Test
  void supportsValueAttributeAndPlainRequestMappingWithoutMethod() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @Controller
        public class LegacyController {

          @RequestMapping(value = "/legacy")
          public String legacy() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).containsExactly(new Endpoint(HttpMethod.REQUEST, "/legacy", "com.example.LegacyController"));
  }

  @Test
  void returnsEmptyListForControllerWithoutMappedMethods() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        public class EmptyController {

          public String notMapped() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).isEmpty();
  }

  @Test
  void ignoresClassesThatAreNotControllers() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;
        import org.springframework.stereotype.Service;

        @Service
        public class UserService {

          @GetMapping("/users")
          public String notAnEndpoint() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).isEmpty();
  }

  @Test
  void normalizesMissingLeadingSlashAndTrailingSlashOnClassPath() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        @RequestMapping("/api/")
        public class UserController {

          @GetMapping("users")
          public String list() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).containsExactly(new Endpoint(HttpMethod.GET, "/api/users", "com.example.UserController"));
  }

  @Test
  void analyzesAllJavaFilesUnderADirectoryRecursively(@TempDir final Path repoRoot) throws IOException {
    final Path controllerDir = repoRoot.resolve("src/main/java/com/example");
    Files.createDirectories(controllerDir);
    Files.writeString(controllerDir.resolve("UserController.java"), """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        public class UserController {

          @GetMapping("/users")
          public String list() { return ""; }
        }
        """);
    Files.writeString(controllerDir.resolve("OrderController.java"), """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        public class OrderController {

          @PostMapping("/orders")
          public String create() { return ""; }
        }
        """);

    final List<Endpoint> endpoints = analyzer.analyze(repoRoot);

    assertThat(endpoints).extracting(Endpoint::method, Endpoint::path, Endpoint::owner)
        .containsExactlyInAnyOrder(
            tuple(HttpMethod.GET, "/users", "com.example.UserController"),
            tuple(HttpMethod.POST, "/orders", "com.example.OrderController"));
  }
}
