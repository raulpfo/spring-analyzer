package io.github.springanalyzer.analyzers.spring;

import io.github.springanalyzer.analyzers.AnalysisException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

final class JavaSourceFiles {

  private JavaSourceFiles() {
  }

  static List<String> readAllUnder(final Path sourceRoot) {
    try (Stream<Path> files = Files.walk(sourceRoot)) {
      return files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
          .map(JavaSourceFiles::read)
          .toList();
    } catch (IOException e) {
      throw new AnalysisException("No se pudo recorrer el codigo fuente en " + sourceRoot, e);
    }
  }

  private static String read(final Path file) {
    try {
      return Files.readString(file);
    } catch (IOException e) {
      throw new AnalysisException("No se pudo leer el fichero " + file, e);
    }
  }
}
