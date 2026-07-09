package io.github.springanalyzer.scm.git;

import io.github.springanalyzer.domain.entities.RepoDefinition;
import io.github.springanalyzer.domain.entities.ScmProvider;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.util.FileUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
public class GitCloner {

  public Path clone(final RepoDefinition repo, final Optional<String> token) {
    final Path targetDir = createTempDir(repo);

    final CloneCommand cloneCommand = Git.cloneRepository()
        .setURI(repo.url())
        .setDirectory(targetDir.toFile())
        .setDepth(1)
        .setCloneAllBranches(false);

    if (repo.branch() != null && !repo.branch().isBlank()) {
      cloneCommand.setBranch(repo.branch());
    }
    token.ifPresent(t -> cloneCommand.setCredentialsProvider(credentialsProvider(repo.provider(), t)));

    try (Git git = cloneCommand.call()) {
      return targetDir;
    } catch (GitAPIException e) {
      cleanup(targetDir);
      throw new GitCloneException("No se pudo clonar el repositorio " + repo.url(), e);
    }
  }

  public void cleanup(final Path directory) {
    if (directory == null || !Files.exists(directory)) {
      return;
    }
    try {
      FileUtils.delete(directory.toFile(), FileUtils.RECURSIVE | FileUtils.RETRY);
    } catch (IOException e) {
      throw new GitCloneException("No se pudo limpiar el directorio temporal " + directory, e);
    }
  }

  private Path createTempDir(final RepoDefinition repo) {
    try {
      return Files.createTempDirectory("spring-analyzer-" + sanitize(repo) + "-");
    } catch (IOException e) {
      throw new GitCloneException("No se pudo crear un directorio temporal para " + repo.url(), e);
    }
  }

  private String sanitize(final RepoDefinition repo) {
    return repo.repoName().replaceAll("[^a-zA-Z0-9._-]", "_");
  }

  private CredentialsProvider credentialsProvider(final ScmProvider provider, final String token) {
    return switch (provider) {
      case GITHUB -> new UsernamePasswordCredentialsProvider(token, "");
      case GITLAB -> new UsernamePasswordCredentialsProvider("oauth2", token);
    };
  }
}
