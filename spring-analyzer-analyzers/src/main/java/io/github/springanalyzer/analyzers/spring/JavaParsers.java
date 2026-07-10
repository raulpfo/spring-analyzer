package io.github.springanalyzer.analyzers.spring;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

final class JavaParsers {

  private JavaParsers() {
  }

  static CompilationUnit parse(final String javaSource) {
    // StaticJavaParser.getParserConfiguration() esta respaldada por un ThreadLocal interno de
    // JavaParser: configurarla una unica vez (p.ej. en un bloque estatico) solo afecta al hilo
    // que lo ejecuta. Como los repos se analizan en un pool de varios hilos, hay que fijar el
    // nivel de lenguaje en cada llamada para que todos los hilos parseen sintaxis Java 14+
    // (pattern matching con instanceof, records, sealed classes...), habitual en Spring Boot moderno.
    StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    return StaticJavaParser.parse(javaSource);
  }
}
