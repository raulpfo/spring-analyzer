package io.github.springanalyzer.boot;

import io.github.springanalyzer.commands.AnalyzeCommand;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication(scanBasePackages = "io.github.springanalyzer")
public class SpringAnalyzerApplication {

  public static void main(String[] args) {
    System.exit(SpringApplication.exit(SpringApplication.run(SpringAnalyzerApplication.class, args)));
  }

  @Bean
  public CommandLineRunner commandLineRunner(AnalyzeCommand analyzeCommand, IFactory factory) {
    return args -> {
      int exitCode = new CommandLine(analyzeCommand, factory)
          .setCaseInsensitiveEnumValuesAllowed(true)
          .execute(args);
      System.exit(exitCode);
    };
  }

}