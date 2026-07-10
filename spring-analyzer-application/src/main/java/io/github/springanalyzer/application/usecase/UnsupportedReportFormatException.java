package io.github.springanalyzer.application.usecase;

import io.github.springanalyzer.domain.entities.ReportFormat;

public class UnsupportedReportFormatException extends RuntimeException {

  public UnsupportedReportFormatException(final ReportFormat format) {
    super("El formato de reporte " + format + " aun no esta soportado");
  }
}
