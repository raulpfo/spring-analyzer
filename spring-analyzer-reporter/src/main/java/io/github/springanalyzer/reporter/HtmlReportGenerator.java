package io.github.springanalyzer.reporter;

import io.github.springanalyzer.core.analyzer.DependencyGraph;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class HtmlReportGenerator {

  private final SpringTemplateEngine templateEngine;
  private final GraphDataRenderer graphDataRenderer;
  private final VersionAnomalyDetector versionAnomalyDetector;
  private final String visNetworkJs;

  public HtmlReportGenerator() {
    this.graphDataRenderer = new GraphDataRenderer();
    this.versionAnomalyDetector = new VersionAnomalyDetector();
    this.templateEngine = new SpringTemplateEngine();
    this.templateEngine.setTemplateResolver(templateResolver());
    this.visNetworkJs = readVisNetworkJs();
  }

  public String generate(final DependencyGraph graph) {
    if (graph == null) {
      throw new IllegalArgumentException("El grafo de dependencias no puede ser nulo");
    }
    final List<OutdatedVersion> outdatedVersions = versionAnomalyDetector.detect(graph.nodes());

    final Context context = new Context();
    context.setVariable("nodes", graph.nodes());
    context.setVariable("orphanEndpoints", graph.orphanEndpoints());
    context.setVariable("orphanConsumptions", graph.orphanConsumptions());
    context.setVariable("outdatedVersions", outdatedVersions);
    context.setVariable("graphData", escapeForScriptTag(graphDataRenderer.render(graph, outdatedVersions)));
    context.setVariable("visNetworkJs", visNetworkJs);

    final StringWriter writer = new StringWriter();
    templateEngine.process("report", context, writer);
    return writer.toString();
  }

  private static ClassLoaderTemplateResolver templateResolver() {
    final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    return resolver;
  }

  private static String escapeForScriptTag(final String json) {
    // Un nombre de servicio con la subcadena "</script>" cerraria prematuramente
    // el <script type="application/json"> donde se embebe el JSON del grafo.
    return json.replace("</", "<\\/");
  }

  private static String readVisNetworkJs() {
    try {
      return new ClassPathResource("static/vis-network.min.js").getContentAsString(StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException("No se pudo cargar la libreria vis-network embebida", e);
    }
  }
}
