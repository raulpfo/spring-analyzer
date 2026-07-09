package io.github.springanalyzer.analyzers.version;

import io.github.springanalyzer.core.analyzer.Dependency;
import io.github.springanalyzer.core.analyzer.ServiceVersionInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class MavenPomParser {

  private MavenPomParser() {
  }

  static ServiceVersionInfo parse(final String pomXml) {
    return parseDocument(pomXml)
        .map(document -> new ServiceVersionInfo(springBootVersionOf(document), javaVersionOf(document),
            dependenciesOf(document)))
        .orElseGet(ServiceVersionInfo::unknown);
  }

  private static Optional<Document> parseDocument(final String pomXml) {
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      // Deshabilita DOCTYPE para evitar ataques XXE al parsear XML de repos externos no confiables
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      final DocumentBuilder builder = factory.newDocumentBuilder();
      // El manejador de errores por defecto escribe en stderr; el XML invalido ya se gestiona con el catch de abajo
      builder.setErrorHandler(new DefaultHandler());
      return Optional.of(builder.parse(new InputSource(new StringReader(pomXml))));
    } catch (ParserConfigurationException | SAXException | IOException e) {
      return Optional.empty();
    }
  }

  private static String springBootVersionOf(final Document document) {
    final Element project = document.getDocumentElement();
    return childElement(project, "parent")
        .filter(parent -> textOf(parent, "groupId").filter("org.springframework.boot"::equals).isPresent())
        .flatMap(parent -> textOf(parent, "version"))
        .or(() -> childElement(project, "properties").flatMap(properties -> textOf(properties, "spring-boot.version")))
        .orElse(null);
  }

  private static String javaVersionOf(final Document document) {
    final Element project = document.getDocumentElement();
    return childElement(project, "properties")
        .flatMap(properties -> textOf(properties, "java.version")
            .or(() -> textOf(properties, "maven.compiler.release"))
            .or(() -> textOf(properties, "maven.compiler.source")))
        .orElse(null);
  }

  private static List<Dependency> dependenciesOf(final Document document) {
    final Element project = document.getDocumentElement();
    return childElement(project, "dependencies")
        .map(dependenciesElement -> childElements(dependenciesElement, "dependency").stream()
            .flatMap(dependency -> toDependency(dependency).stream())
            .toList())
        .orElseGet(List::of);
  }

  private static Optional<Dependency> toDependency(final Element dependencyElement) {
    final Optional<String> groupId = textOf(dependencyElement, "groupId");
    final Optional<String> artifactId = textOf(dependencyElement, "artifactId");
    if (groupId.isEmpty() || artifactId.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new Dependency(groupId.get(), artifactId.get(), textOf(dependencyElement, "version").orElse(null)));
  }

  private static Optional<Element> childElement(final Element parent, final String tagName) {
    final List<Element> children = childElements(parent, tagName);
    return children.isEmpty() ? Optional.empty() : Optional.of(children.get(0));
  }

  private static List<Element> childElements(final Element parent, final String tagName) {
    final List<Element> result = new ArrayList<>();
    final NodeList children = parent.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      final Node node = children.item(i);
      if (node instanceof Element element && element.getTagName().equals(tagName)) {
        result.add(element);
      }
    }
    return result;
  }

  private static Optional<String> textOf(final Element parent, final String tagName) {
    return childElement(parent, tagName)
        .map(Element::getTextContent)
        .map(String::trim)
        .filter(text -> !text.isBlank());
  }
}
