package io.github.springanalyzer.application.usecase;

import io.github.springanalyzer.domain.entities.CommandConfig;
import io.github.springanalyzer.domain.usecase.LaunchSpringAnalyzeUseCase;
import org.springframework.stereotype.Component;

@Component
public class LaunchSpringAnalyzeUseCaseImpl implements LaunchSpringAnalyzeUseCase {
  @Override
  public String run(CommandConfig command) {
    try { Thread.sleep(8000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    return "Todo un exito";
  }
}
