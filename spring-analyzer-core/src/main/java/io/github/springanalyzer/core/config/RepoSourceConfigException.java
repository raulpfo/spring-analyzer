package io.github.springanalyzer.core.config;

public class RepoSourceConfigException extends RuntimeException {
  public RepoSourceConfigException(final String message) {
    super(message);
  }

  public RepoSourceConfigException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
