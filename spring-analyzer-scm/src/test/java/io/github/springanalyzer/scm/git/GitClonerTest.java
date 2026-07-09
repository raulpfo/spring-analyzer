package io.github.springanalyzer.scm.git;

import io.github.springanalyzer.domain.entities.RepoDefinition;
import io.github.springanalyzer.domain.entities.ScmProvider;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitClonerTest {

  private final GitCloner gitCloner = new GitCloner();

  @Test
  void clonesPublicRepoWithoutToken(@TempDir final Path sourceDir) throws Exception {
    initFixtureRepo(sourceDir, "main").close();

    final RepoDefinition repo = new RepoDefinition(sourceDir.toUri().toString(), null, ScmProvider.GITHUB);

    final Path clonedDir = gitCloner.clone(repo, Optional.empty());

    try {
      assertThat(clonedDir.resolve("README.md")).exists();
      assertThat(clonedDir.resolve(".git")).exists();
    } finally {
      gitCloner.cleanup(clonedDir);
    }
  }

  @Test
  void clonesSpecificBranchWhenRequested(@TempDir final Path sourceDir) throws Exception {
    try (Git git = initFixtureRepo(sourceDir, "main")) {
      git.branchCreate().setName("feature").call();
      git.checkout().setName("feature").call();
      Files.writeString(sourceDir.resolve("feature.txt"), "solo en feature");
      git.add().addFilepattern(".").call();
      git.commit().setMessage("commit en feature").call();
    }

    final RepoDefinition repo = new RepoDefinition(sourceDir.toUri().toString(), "feature", ScmProvider.GITHUB);

    final Path clonedDir = gitCloner.clone(repo, Optional.empty());

    try {
      assertThat(clonedDir.resolve("feature.txt")).exists();
    } finally {
      gitCloner.cleanup(clonedDir);
    }
  }

  @Test
  void clonesDefaultBranchWhenBranchIsBlank(@TempDir final Path sourceDir) throws Exception {
    initFixtureRepo(sourceDir, "main").close();

    final RepoDefinition repo = new RepoDefinition(sourceDir.toUri().toString(), "   ", ScmProvider.GITHUB);

    final Path clonedDir = gitCloner.clone(repo, Optional.empty());

    try {
      assertThat(clonedDir.resolve("README.md")).exists();
    } finally {
      gitCloner.cleanup(clonedDir);
    }
  }

  @Test
  void clonesSuccessfullyWhenTokenIsProvidedForLocalTransport(@TempDir final Path sourceDir) throws Exception {
    initFixtureRepo(sourceDir, "main").close();

    final RepoDefinition repo = new RepoDefinition(sourceDir.toUri().toString(), null, ScmProvider.GITLAB);

    final Path clonedDir = gitCloner.clone(repo, Optional.of("some-token"));

    try {
      assertThat(clonedDir.resolve("README.md")).exists();
    } finally {
      gitCloner.cleanup(clonedDir);
    }
  }

  @Test
  void throwsGitCloneExceptionWhenUrlIsInvalid() {
    final RepoDefinition repo = new RepoDefinition("not-a-valid-repo-url", null, ScmProvider.GITHUB);

    assertThatThrownBy(() -> gitCloner.clone(repo, Optional.empty()))
        .isInstanceOf(GitCloneException.class);
  }

  @Test
  void cleanupRemovesTheTemporaryDirectory(@TempDir final Path sourceDir) throws Exception {
    initFixtureRepo(sourceDir, "main").close();
    final RepoDefinition repo = new RepoDefinition(sourceDir.toUri().toString(), null, ScmProvider.GITHUB);
    final Path clonedDir = gitCloner.clone(repo, Optional.empty());

    gitCloner.cleanup(clonedDir);

    assertThat(clonedDir).doesNotExist();
  }

  private Git initFixtureRepo(final Path directory, final String initialBranch) throws Exception {
    final Git git = Git.init().setDirectory(directory.toFile()).setInitialBranch(initialBranch).call();
    Files.writeString(directory.resolve("README.md"), "fixture repo");
    git.add().addFilepattern(".").call();
    git.commit().setMessage("commit inicial").call();
    return git;
  }
}
