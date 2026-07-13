package io.github.springanalyzer.analyzers.spring;

import io.github.springanalyzer.core.analyzer.Endpoint;
import io.github.springanalyzer.core.analyzer.HttpMethod;
import io.github.springanalyzer.domain.entities.CustomAnnotationsConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
  void detectsEndpointsUsingCustomControllerAndMappingAnnotations() {
    final String source = """
        package com.example;

        import com.acme.fwk.MiController;
        import com.acme.fwk.MiGet;

        @MiController
        public class UserController {

          @MiGet("/users/{id}")
          public String get() { return ""; }
        }
        """;
    final CustomAnnotationsConfig customAnnotations = new CustomAnnotationsConfig(
        List.of("com.acme.fwk.MiController"), Map.of("GET", List.of("com.acme.fwk.MiGet")), List.of());

    final List<Endpoint> endpoints = analyzer.analyzeSource(source, customAnnotations);

    assertThat(endpoints)
        .containsExactly(new Endpoint(HttpMethod.GET, "/users/{id}", "com.example.UserController"));
  }

  @Test
  void mixesStandardAndCustomMappingAnnotationsOnTheSameController() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;
        import com.acme.fwk.MiPost;

        @RestController
        public class UserController {

          @GetMapping("/users")
          public String list() { return ""; }

          @MiPost("/users")
          public String create() { return ""; }
        }
        """;
    final CustomAnnotationsConfig customAnnotations =
        new CustomAnnotationsConfig(List.of(), Map.of("POST", List.of("com.acme.fwk.MiPost")), List.of());

    final List<Endpoint> endpoints = analyzer.analyzeSource(source, customAnnotations);

    assertThat(endpoints).extracting(Endpoint::method, Endpoint::path, Endpoint::owner)
        .containsExactlyInAnyOrder(
            tuple(HttpMethod.GET, "/users", "com.example.UserController"),
            tuple(HttpMethod.POST, "/users", "com.example.UserController"));
  }

  @Test
  void ignoresCustomAnnotationsWhenNotDeclaredAsControllers() {
    final String source = """
        package com.example;

        import com.acme.fwk.MiController;
        import com.acme.fwk.MiGet;

        @MiController
        public class UserController {

          @MiGet("/users/{id}")
          public String get() { return ""; }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source, CustomAnnotationsConfig.EMPTY);

    assertThat(endpoints).isEmpty();
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

  @Test
  void parsesControllersUsingModernJavaSyntaxLikePatternMatchingInstanceof() {
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        public class UserController {

          @GetMapping("/users/{id}")
          public String get(Object id) {
            if (id instanceof String stringId) {
              return stringId;
            }
            return String.valueOf(id);
          }
        }
        """;

    final List<Endpoint> endpoints = analyzer.analyzeSource(source);

    assertThat(endpoints).containsExactly(new Endpoint(HttpMethod.GET, "/users/{id}", "com.example.UserController"));
  }

  @Test
  void parsesRecordDeclarationsRegardlessOfWhichThreadRunsTheParsing() throws InterruptedException {
    // StaticJavaParser.getParserConfiguration() esta respaldada por un ThreadLocal: si el nivel
    // de lenguaje solo se configurase una vez (p.ej. en un bloque estatico), solo el hilo que
    // ejecuta esa inicializacion quedaria bien configurado. Para que este test detecte esa
    // regresion sin depender de que otro test haya "calentado" antes esa inicializacion en el
    // hilo principal (lo que lo haria pasar incluso con el bug), forzamos aqui esa
    // inicializacion en el hilo actual y comprobamos que un hilo nuevo, que nunca la ha
    // ejecutado, tambien parsea correctamente sintaxis Java 14+ como los records.
    final String source = """
        package com.example;

        import org.springframework.web.bind.annotation.*;

        @RestController
        public class UserController {

          record UserId(String value) { }

          @GetMapping("/users/{id}")
          public String get() { return ""; }
        }
        """;

    analyzer.analyzeSource(source);

    final AtomicReference<List<Endpoint>> endpoints = new AtomicReference<>();
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    final Thread thread = new Thread(() -> {
      try {
        endpoints.set(analyzer.analyzeSource(source));
      } catch (final Throwable t) {
        failure.set(t);
      }
    });
    thread.start();
    thread.join();

    assertThat(failure.get()).isNull();
    assertThat(endpoints.get()).containsExactly(new Endpoint(HttpMethod.GET, "/users/{id}", "com.example.UserController"));
  }
}
