package io.github.springanalyzer.analyzers.spring;

import io.github.springanalyzer.core.analyzer.Endpoint;
import io.github.springanalyzer.core.analyzer.HttpMethod;

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
    return JavaSourceFiles.readAllUnder(sourceRoot).stream()
        .flatMap(source -> analyzeSource(source).stream())
        .toList();
  }

  public List<Endpoint> analyzeSource(final String javaSource) {
    final CompilationUnit compilationUnit = JavaParsers.parse(javaSource);
    return compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
        .filter(this::isController)
        .flatMap(this::endpointsOf)
        .toList();
  }

  private boolean isController(final ClassOrInterfaceDeclaration classDeclaration) {
    return classDeclaration.getAnnotationByName("RestController").isPresent()
        || classDeclaration.getAnnotationByName("Controller").isPresent();
  }

  private Stream<Endpoint> endpointsOf(final ClassOrInterfaceDeclaration classDeclaration) {
    final String owner = classDeclaration.getFullyQualifiedName().orElseGet(classDeclaration::getNameAsString);
    final String classPath = classDeclaration.getAnnotationByName("RequestMapping")
        .flatMap(MappingAnnotations::pathOf)
        .orElse("");

    return classDeclaration.getMethods().stream()
        .flatMap(this::mappingAnnotationsOf)
        .map(annotation -> toEndpoint(annotation, classPath, owner));
  }

  private Stream<AnnotationExpr> mappingAnnotationsOf(final MethodDeclaration method) {
    return method.getAnnotations().stream()
        .filter(annotation -> MappingAnnotations.httpMethodOf(annotation.getNameAsString()).isPresent());
  }

  private Endpoint toEndpoint(final AnnotationExpr annotation, final String classPath, final String owner) {
    final HttpMethod httpMethod = MappingAnnotations.httpMethodOf(annotation.getNameAsString()).orElseThrow();
    final String methodPath = MappingAnnotations.pathOf(annotation).orElse("");
    return new Endpoint(httpMethod, MappingAnnotations.joinPaths(classPath, methodPath), owner);
  }
}
