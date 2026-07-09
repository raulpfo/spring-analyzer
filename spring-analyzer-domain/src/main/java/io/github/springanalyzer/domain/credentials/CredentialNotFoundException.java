package io.github.springanalyzer.domain.credentials;

import io.github.springanalyzer.domain.entities.ScmProvider;

public class CredentialNotFoundException extends RuntimeException {
  public CredentialNotFoundException(final ScmProvider provider) {
    super("No se encontro credencial para " + provider + ". Usa --github-token/--gitlab-token, --token-env, "
        + "o define la variable de entorno " + provider.defaultTokenEnvVar() + ".");
  }
}
