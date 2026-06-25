package io.github.springanalyzer.domain.usecase;

import io.github.springanalyzer.domain.entities.CommandConfig;

public interface LaunchSpringAnalyzeUseCase {
  String run(final CommandConfig command);
}
