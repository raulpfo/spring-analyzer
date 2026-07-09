package io.github.springanalyzer.domain.credentials;

import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.entities.ScmProvider;

import java.util.Optional;
import java.util.function.Function;

public class CredentialResolver {

  private final CommandConfig commandConfig;
  private final Function<String, String> envLookup;

  public CredentialResolver(final CommandConfig commandConfig) {
    this(commandConfig, System::getenv);
  }

  CredentialResolver(final CommandConfig commandConfig, final Function<String, String> envLookup) {
    this.commandConfig = commandConfig;
    this.envLookup = envLookup;
  }

  public Optional<String> resolve(final ScmProvider provider) {
    final String explicitToken = switch (provider) {
      case GITHUB -> commandConfig.githubToken();
      case GITLAB -> commandConfig.gitlabToken();
    };
    if (isNotBlank(explicitToken)) {
      return Optional.of(explicitToken);
    }

    if (isNotBlank(commandConfig.tokenEnv())) {
      final String fromCustomEnv = envLookup.apply(commandConfig.tokenEnv());
      if (isNotBlank(fromCustomEnv)) {
        return Optional.of(fromCustomEnv);
      }
    }

    final String fromDefaultEnv = envLookup.apply(provider.defaultTokenEnvVar());
    return isNotBlank(fromDefaultEnv) ? Optional.of(fromDefaultEnv) : Optional.empty();
  }

  public String resolveRequired(final ScmProvider provider) {
    return resolve(provider).orElseThrow(() -> new CredentialNotFoundException(provider));
  }

  private static boolean isNotBlank(final String value) {
    return value != null && !value.isBlank();
  }
}
