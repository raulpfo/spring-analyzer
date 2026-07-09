package io.github.springanalyzer.analyzers.spring;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;

import java.util.Optional;

final class MappingAnnotations {

  private MappingAnnotations() {
  }

  static Optional<HttpMethod> httpMethodOf(final String annotationName) {
    return switch (annotationName) {
      case "GetMapping" -> Optional.of(HttpMethod.GET);
      case "PostMapping" -> Optional.of(HttpMethod.POST);
      case "PutMapping" -> Optional.of(HttpMethod.PUT);
      case "DeleteMapping" -> Optional.of(HttpMethod.DELETE);
      case "RequestMapping" -> Optional.of(HttpMethod.REQUEST);
      default -> Optional.empty();
    };
  }

  static Optional<String> pathOf(final AnnotationExpr annotation) {
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

  static Optional<String> literalStringOf(final Expression expression) {
    if (expression.isStringLiteralExpr()) {
      return Optional.of(expression.asStringLiteralExpr().asString());
    }
    if (expression.isArrayInitializerExpr() && !expression.asArrayInitializerExpr().getValues().isEmpty()) {
      return literalStringOf(expression.asArrayInitializerExpr().getValues().get(0));
    }
    return Optional.empty();
  }

  static String joinPaths(final String classPath, final String methodPath) {
    final String combined = normalize(classPath) + normalize(methodPath);
    return combined.isEmpty() ? "/" : combined;
  }

  static String normalize(final String rawPath) {
    if (rawPath == null || rawPath.isBlank()) {
      return "";
    }
    final String withLeadingSlash = rawPath.startsWith("/") ? rawPath : "/" + rawPath;
    return withLeadingSlash.endsWith("/")
        ? withLeadingSlash.substring(0, withLeadingSlash.length() - 1)
        : withLeadingSlash;
  }
}
