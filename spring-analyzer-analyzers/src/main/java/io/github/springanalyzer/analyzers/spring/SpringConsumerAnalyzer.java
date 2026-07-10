package io.github.springanalyzer.analyzers.spring;

import io.github.springanalyzer.core.analyzer.EndpointConsumption;
import io.github.springanalyzer.core.analyzer.HttpMethod;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class SpringConsumerAnalyzer {

  private static final List<String> HTTP_SCHEMES = List.of("http://", "https://");

  private static final Map<String, HttpMethod> REST_TEMPLATE_METHODS = Map.of(
      "getForObject", HttpMethod.GET,
      "getForEntity", HttpMethod.GET,
      "postForObject", HttpMethod.POST,
      "postForEntity", HttpMethod.POST,
      "put", HttpMethod.PUT,
      "delete", HttpMethod.DELETE);

  private static final Map<String, HttpMethod> WEB_CLIENT_VERBS = Map.of(
      "get", HttpMethod.GET,
      "post", HttpMethod.POST,
      "put", HttpMethod.PUT,
      "delete", HttpMethod.DELETE);

  public List<EndpointConsumption> analyze(final Path sourceRoot) {
    return JavaSourceFiles.readAllUnder(sourceRoot).stream()
        .flatMap(source -> analyzeSource(source).stream())
        .toList();
  }

  public List<EndpointConsumption> analyzeSource(final String javaSource) {
    final CompilationUnit compilationUnit = JavaParsers.parse(javaSource);
    final Map<String, String> variableTypes = variableTypesIn(compilationUnit);

    final Stream<EndpointConsumption> feignConsumptions =
        compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream().flatMap(this::feignConsumptionsOf);
    return Stream.concat(feignConsumptions,
            Stream.concat(restTemplateConsumptionsOf(compilationUnit, variableTypes),
                webClientConsumptionsOf(compilationUnit, variableTypes)))
        .toList();
  }

  private Stream<EndpointConsumption> feignConsumptionsOf(final ClassOrInterfaceDeclaration typeDeclaration) {
    return typeDeclaration.getAnnotationByName("FeignClient")
        .map(feignAnnotation -> {
          final String targetService = feignServiceNameOf(feignAnnotation).orElse(null);
          final String classPath = typeDeclaration.getAnnotationByName("RequestMapping")
              .flatMap(MappingAnnotations::pathOf)
              .orElse("");

          return typeDeclaration.getMethods().stream()
              .flatMap(method -> method.getAnnotations().stream()
                  .flatMap(annotation -> MappingAnnotations.httpMethodOf(annotation.getNameAsString())
                      .map(httpMethod -> new EndpointConsumption(targetService,
                          MappingAnnotations.joinPaths(classPath, MappingAnnotations.pathOf(annotation).orElse("")),
                          httpMethod))
                      .stream()));
        })
        .orElseGet(Stream::empty);
  }

  private Optional<String> feignServiceNameOf(final AnnotationExpr annotation) {
    if (annotation instanceof SingleMemberAnnotationExpr singleMember) {
      return MappingAnnotations.literalStringOf(singleMember.getMemberValue());
    }
    if (annotation instanceof NormalAnnotationExpr normal) {
      return normal.getPairs().stream()
          .filter(pair -> pair.getNameAsString().equals("name") || pair.getNameAsString().equals("value"))
          .findFirst()
          .flatMap(pair -> MappingAnnotations.literalStringOf(pair.getValue()));
    }
    return Optional.empty();
  }

  private Stream<EndpointConsumption> restTemplateConsumptionsOf(final CompilationUnit compilationUnit,
      final Map<String, String> variableTypes) {
    return compilationUnit.findAll(MethodCallExpr.class).stream()
        .filter(call -> isCallOnType(call, variableTypes, "RestTemplate"))
        .flatMap(call -> restTemplateConsumptionOf(call).stream());
  }

  private Optional<EndpointConsumption> restTemplateConsumptionOf(final MethodCallExpr call) {
    final String methodName = call.getNameAsString();
    if ("exchange".equals(methodName) && call.getArguments().size() >= 2) {
      return httpMethodFromExchangeArgument(call.getArgument(1))
          .flatMap(httpMethod -> resolvePathExpression(call.getArgument(0)).map(url -> toConsumption(url, httpMethod)));
    }
    final HttpMethod httpMethod = REST_TEMPLATE_METHODS.get(methodName);
    if (httpMethod == null || call.getArguments().isEmpty()) {
      return Optional.empty();
    }
    return resolvePathExpression(call.getArgument(0)).map(url -> toConsumption(url, httpMethod));
  }

  private Optional<HttpMethod> httpMethodFromExchangeArgument(final Expression expression) {
    if (!expression.isFieldAccessExpr()) {
      return Optional.empty();
    }
    return switch (expression.asFieldAccessExpr().getNameAsString()) {
      case "GET" -> Optional.of(HttpMethod.GET);
      case "POST" -> Optional.of(HttpMethod.POST);
      case "PUT" -> Optional.of(HttpMethod.PUT);
      case "DELETE" -> Optional.of(HttpMethod.DELETE);
      default -> Optional.empty();
    };
  }

  private Stream<EndpointConsumption> webClientConsumptionsOf(final CompilationUnit compilationUnit,
      final Map<String, String> variableTypes) {
    return compilationUnit.findAll(MethodCallExpr.class).stream()
        .filter(call -> call.getNameAsString().equals("uri") && !call.getArguments().isEmpty())
        .flatMap(uriCall -> webClientConsumptionOf(uriCall, variableTypes).stream());
  }

  private Optional<EndpointConsumption> webClientConsumptionOf(final MethodCallExpr uriCall,
      final Map<String, String> variableTypes) {
    return uriCall.getScope()
        .filter(Expression::isMethodCallExpr)
        .map(Expression::asMethodCallExpr)
        .filter(verbCall -> isCallOnType(verbCall, variableTypes, "WebClient"))
        .flatMap(verbCall -> Optional.ofNullable(WEB_CLIENT_VERBS.get(verbCall.getNameAsString())))
        .flatMap(httpMethod -> resolvePathExpression(uriCall.getArgument(0)).map(url -> toConsumption(url, httpMethod)));
  }

  private boolean isCallOnType(final MethodCallExpr call, final Map<String, String> variableTypes, final String typeName) {
    return call.getScope()
        .filter(Expression::isNameExpr)
        .map(scope -> variableTypes.get(scope.asNameExpr().getNameAsString()))
        .filter(typeName::equals)
        .isPresent();
  }

  private Map<String, String> variableTypesIn(final Node scope) {
    final Map<String, String> types = new HashMap<>();
    scope.findAll(FieldDeclaration.class)
        .forEach(field -> field.getVariables()
            .forEach(variable -> types.put(variable.getNameAsString(), field.getElementType().asString())));
    scope.findAll(Parameter.class)
        .forEach(parameter -> types.put(parameter.getNameAsString(), parameter.getType().asString()));
    scope.findAll(VariableDeclarationExpr.class)
        .forEach(declaration -> declaration.getVariables()
            .forEach(variable -> types.put(variable.getNameAsString(), variable.getType().asString())));
    return types;
  }

  private static Optional<String> resolvePathExpression(final Expression expression) {
    if (expression.isStringLiteralExpr()) {
      return Optional.of(expression.asStringLiteralExpr().asString());
    }
    if (expression.isBinaryExpr() && expression.asBinaryExpr().getOperator() == BinaryExpr.Operator.PLUS) {
      final BinaryExpr binary = expression.asBinaryExpr();
      final Optional<String> left = resolvePathExpression(binary.getLeft());
      final Optional<String> right = resolvePathExpression(binary.getRight());
      if (left.isEmpty() && right.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(left.orElse("{}") + right.orElse("{}"));
    }
    return Optional.empty();
  }

  private EndpointConsumption toConsumption(final String resolvedUrl, final HttpMethod httpMethod) {
    if (hasHttpScheme(resolvedUrl)) {
      return new EndpointConsumption(hostOf(resolvedUrl).orElse(null), pathAfterScheme(resolvedUrl), httpMethod);
    }
    final String path = MappingAnnotations.normalize(resolvedUrl);
    return new EndpointConsumption(null, path.isEmpty() ? "/" : path, httpMethod);
  }

  private static boolean hasHttpScheme(final String url) {
    return HTTP_SCHEMES.stream().anyMatch(url::startsWith);
  }

  private static Optional<String> hostOf(final String url) {
    return withoutScheme(url)
        .map(rest -> {
          final int slashIndex = rest.indexOf('/');
          return slashIndex >= 0 ? rest.substring(0, slashIndex) : rest;
        })
        .filter(host -> !host.isBlank() && !host.contains("{}"));
  }

  private static String pathAfterScheme(final String url) {
    final String rest = withoutScheme(url).orElse(url);
    final int slashIndex = rest.indexOf('/');
    final String path = slashIndex >= 0 ? rest.substring(slashIndex) : "";
    return path.isEmpty() ? "/" : path;
  }

  private static Optional<String> withoutScheme(final String url) {
    return HTTP_SCHEMES.stream()
        .filter(url::startsWith)
        .findFirst()
        .map(scheme -> url.substring(scheme.length()));
  }
}
