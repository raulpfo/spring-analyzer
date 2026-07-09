package io.github.springanalyzer.scm.git;

public class GitCloneException extends RuntimeException {
  public GitCloneException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
