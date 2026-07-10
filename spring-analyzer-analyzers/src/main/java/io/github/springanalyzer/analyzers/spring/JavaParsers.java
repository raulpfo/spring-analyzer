package io.github.springanalyzer.analyzers.spring;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

final class JavaParsers {

  static {
    // Sin esto, JavaParser usa su nivel de lenguaje por defecto, que rechaza sintaxis
    // introducida en Java 14+ (pattern matching con instanceof, records, sealed classes...),
    // habitual en codigo Spring Boot moderno.
    StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
  }

  private JavaParsers() {
  }

  static CompilationUnit parse(final String javaSource) {
    return StaticJavaParser.parse(javaSource);
  }
}
