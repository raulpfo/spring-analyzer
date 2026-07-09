package io.github.springanalyzer.analyzers;

public class AnalysisException extends RuntimeException {

  public AnalysisException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
