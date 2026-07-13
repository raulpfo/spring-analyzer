package io.github.springanalyzer.analyzers.spring;

import io.github.springanalyzer.core.analyzer.Endpoint;
import io.github.springanalyzer.core.analyzer.HttpMethod;
import io.github.springanalyzer.domain.entities.CustomAnnotationsConfig;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
public class SpringJavaEndpointAnalyzer {

  public List<Endpoint> analyze(final Path sourceRoot) {
    return analyze(sourceRoot, CustomAnnotationsConfig.EMPTY);
  }

  public List<Endpoint> analyze(final Path sourceRoot, final CustomAnnotationsConfig customAnnotations) {
    return JavaSourceFiles.readAllUnder(sourceRoot).stream()
        .flatMap(source -> analyzeSource(source, customAnnotations).stream())
        .toList();
  }

  public List<Endpoint> analyzeSource(final String javaSource) {
    return analyzeSource(javaSource, CustomAnnotationsConfig.EMPTY);
  }

  public List<Endpoint> analyzeSource(final String javaSource, final CustomAnnotationsConfig customAnnotations) {
    final CompilationUnit compilationUnit = JavaParsers.parse(javaSource);
    return compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
        .filter(classDeclaration -> isController(classDeclaration, customAnnotations))
        .flatMap(classDeclaration -> endpointsOf(classDeclaration, customAnnotations))
        .toList();
  }

  private boolean isController(final ClassOrInterfaceDeclaration classDeclaration,
      final CustomAnnotationsConfig customAnnotations) {
    return classDeclaration.getAnnotationByName("RestController").isPresent()
        || classDeclaration.getAnnotationByName("Controller").isPresent()
        || customAnnotations.controllers().stream()
            .anyMatch(name -> classDeclaration.getAnnotationByName(MappingAnnotations.simpleNameOf(name)).isPresent());
  }

  private Stream<Endpoint> endpointsOf(final ClassOrInterfaceDeclaration classDeclaration,
      final CustomAnnotationsConfig customAnnotations) {
    final String owner = classDeclaration.getFullyQualifiedName().orElseGet(classDeclaration::getNameAsString);
    final String classPath = classDeclaration.getAnnotationByName("RequestMapping")
        .flatMap(MappingAnnotations::pathOf)
        .orElse("");

    return classDeclaration.getMethods().stream()
        .flatMap(method -> mappingAnnotationsOf(method, customAnnotations))
        .map(annotation -> toEndpoint(annotation, classPath, owner, customAnnotations));
  }

  private Stream<AnnotationExpr> mappingAnnotationsOf(final MethodDeclaration method,
      final CustomAnnotationsConfig customAnnotations) {
    return method.getAnnotations().stream()
        .filter(annotation -> MappingAnnotations.httpMethodOf(annotation.getNameAsString(), customAnnotations).isPresent());
  }

  private Endpoint toEndpoint(final AnnotationExpr annotation, final String classPath, final String owner,
      final CustomAnnotationsConfig customAnnotations) {
    final HttpMethod httpMethod =
        MappingAnnotations.httpMethodOf(annotation.getNameAsString(), customAnnotations).orElseThrow();
    final String methodPath = MappingAnnotations.pathOf(annotation).orElse("");
    return new Endpoint(httpMethod, MappingAnnotations.joinPaths(classPath, methodPath), owner);
  }
}
