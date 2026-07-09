package io.github.springanalyzer.core.analyzer;

public enum HttpMethod {
  GET,
  POST,
  PUT,
  DELETE,
  // @RequestMapping sin 'method' explicito: aplica a cualquier verbo HTTP
  REQUEST
}
