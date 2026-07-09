package io.github.springanalyzer.analyzers.spring;

public enum HttpMethod {
  GET,
  POST,
  PUT,
  DELETE,
  // @RequestMapping sin 'method' explicito: aplica a cualquier verbo HTTP
  REQUEST
}
