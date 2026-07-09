package io.github.springanalyzer.analyzers.spring;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class SpringJavaEndpointAnalyzer {

  public List<Endpoint> analyze(final Path sourceRoot) {
    try (Stream<Path> files = Files.walk(sourceRoot)) {
      return files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
          .flatMap(path -> analyzeSource(readSource(path)).stream())
          .toList();
    } catch (IOException e) {
      throw new EndpointAnalysisException("No se pudo recorrer el codigo fuente en " + sourceRoot, e);
    }
  }

  public List<Endpoint> analyzeSource(final String javaSource) {
    final CompilationUnit compilationUnit = StaticJavaParser.parse(javaSource);
    return compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
        .filter(this::isController)
        .flatMap(this::endpointsOf)
        .toList();
  }

  private String readSource(final Path file) {
    try {
      return Files.readString(file);
    } catch (IOException e) {
      throw new EndpointAnalysisException("No se pudo leer el fichero " + file, e);
    }
  }

  private boolean isController(final ClassOrInterfaceDeclaration classDeclaration) {
    return classDeclaration.getAnnotationByName("RestController").isPresent()
        || classDeclaration.getAnnotationByName("Controller").isPresent();
  }

  private Stream<Endpoint> endpointsOf(final ClassOrInterfaceDeclaration classDeclaration) {
    final String owner = classDeclaration.getFullyQualifiedName().orElseGet(classDeclaration::getNameAsString);
    final String classPath = classDeclaration.getAnnotationByName("RequestMapping")
        .flatMap(SpringJavaEndpointAnalyzer::pathOf)
        .orElse("");

    return classDeclaration.getMethods().stream()
        .flatMap(this::mappingAnnotationsOf)
        .map(annotation -> toEndpoint(annotation, classPath, owner));
  }

  private Stream<AnnotationExpr> mappingAnnotationsOf(final MethodDeclaration method) {
    return method.getAnnotations().stream()
        .filter(annotation -> httpMethodOf(annotation.getNameAsString()).isPresent());
  }

  private Endpoint toEndpoint(final AnnotationExpr annotation, final String classPath, final String owner) {
    final HttpMethod httpMethod = httpMethodOf(annotation.getNameAsString()).orElseThrow();
    final String methodPath = pathOf(annotation).orElse("");
    return new Endpoint(httpMethod, joinPaths(classPath, methodPath), owner);
  }

  private static Optional<HttpMethod> httpMethodOf(final String annotationName) {
    return switch (annotationName) {
      case "GetMapping" -> Optional.of(HttpMethod.GET);
      case "PostMapping" -> Optional.of(HttpMethod.POST);
      case "PutMapping" -> Optional.of(HttpMethod.PUT);
      case "DeleteMapping" -> Optional.of(HttpMethod.DELETE);
      case "RequestMapping" -> Optional.of(HttpMethod.REQUEST);
      default -> Optional.empty();
    };
  }

  private static Optional<String> pathOf(final AnnotationExpr annotation) {
    if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
      return literalStringOf(singleMember.getMemberValue());
    }
    if (annotation instanceof NormalAnnotationExpr normal) {
      return normal.getPairs().stream()
          .filter(pair -> pair.getNameAsString().equals("value") || pair.getNameAsString().equals("path"))
          .findFirst()
          .flatMap(pair -> literalStringOf(pair.getValue()));
    }
    return Optional.empty();
  }

  private static Optional<String> literalStringOf(final Expression expression) {
    if (expression.isStringLiteralExpr()) {
      return Optional.of(expression.asStringLiteralExpr().asString());
    }
    if (expression.isArrayInitializerExpr() && !expression.asArrayInitializerExpr().getValues().isEmpty()) {
      return literalStringOf(expression.asArrayInitializerExpr().getValues().get(0));
    }
    return Optional.empty();
  }

  private static String joinPaths(final String classPath, final String methodPath) {
    final String combined = normalize(classPath) + normalize(methodPath);
    return combined.isEmpty() ? "/" : combined;
  }

  private static String normalize(final String rawPath) {
    if (rawPath == null || rawPath.isBlank()) {
      return "";
    }
    final String withLeadingSlash = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
    return withLeadingSlash.endsWith("/")
        ? withLeadingSlash.substring(0, withLeadingSlash.length() - 1)
        : withLeadingSlash;
  }
}
