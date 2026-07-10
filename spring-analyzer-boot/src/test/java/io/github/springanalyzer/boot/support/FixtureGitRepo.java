package io.github.springanalyzer.boot.support;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class FixtureGitRepo {

  private FixtureGitRepo() {
  }

  public static String create(final Path parentDir, final String repoName, final Map<String, String> filesByRelativePath) {
    final Path repoDir = parentDir.resolve(repoName);
    writeFiles(repoDir, filesByRelativePath);
    commitAll(repoDir);
    return repoDir.toUri().toString();
  }

  private static void writeFiles(final Path repoDir, final Map<String, String> filesByRelativePath) {
    try {
      Files.createDirectories(repoDir);
      for (final Map.Entry<String, String> file : filesByRelativePath.entrySet()) {
        final Path filePath = repoDir.resolve(file.getKey());
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, file.getValue());
      }
    } catch (final IOException e) {
      throw new UncheckedIOException("No se pudo escribir el fixture del repositorio en " + repoDir, e);
    }
  }

  private static void commitAll(final Path repoDir) {
    try (Git git = Git.init().setDirectory(repoDir.toFile()).call()) {
      git.add().addFilepattern(".").call();
      git.commit()
          .setMessage("fixture inicial")
          .setAuthor("spring-analyzer-tests", "tests@spring-analyzer.local")
          .setSign(false)
          .call();
    } catch (final GitAPIException e) {
      throw new IllegalStateException("No se pudo preparar el repositorio de fixture en " + repoDir, e);
    }
  }
}
