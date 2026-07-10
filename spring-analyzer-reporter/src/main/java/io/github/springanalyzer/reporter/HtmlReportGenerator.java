package io.github.springanalyzer.reporter;

import io.github.springanalyzer.core.analyzer.DependencyGraph;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.StringWriter;

@Component
public class HtmlReportGenerator {

  private final SpringTemplateEngine templateEngine;
  private final MermaidGraphRenderer mermaidGraphRenderer;
  private final VersionAnomalyDetector versionAnomalyDetector;

  public HtmlReportGenerator() {
    this.mermaidGraphRenderer = new MermaidGraphRenderer();
    this.versionAnomalyDetector = new VersionAnomalyDetector();
    this.templateEngine = new SpringTemplateEngine();
    this.templateEngine.setTemplateResolver(templateResolver());
  }

  public String generate(final DependencyGraph graph) {
    if (graph == null) {
      throw new IllegalArgumentException("El grafo de dependencias no puede ser nulo");
    }
    final Context context = new Context();
    context.setVariable("nodes", graph.nodes());
    context.setVariable("orphanEndpoints", graph.orphanEndpoints());
    context.setVariable("orphanConsumptions", graph.orphanConsumptions());
    context.setVariable("outdatedVersions", versionAnomalyDetector.detect(graph.nodes()));
    context.setVariable("mermaidGraph", mermaidGraphRenderer.render(graph));

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
}
