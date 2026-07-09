package io.github.springanalyzer.core.analyzer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EndpointTest {

  @Test
  void createsWithValidFields() {
    final Endpoint endpoint = new Endpoint(HttpMethod.GET, "/users", "UserController");

    assertThat(endpoint.method()).isEqualTo(HttpMethod.GET);
    assertThat(endpoint.path()).isEqualTo("/users");
    assertThat(endpoint.owner()).isEqualTo("UserController");
  }

  @Test
  void rejectsNullMethod() {
    assertThatThrownBy(() -> new Endpoint(null, "/users", "UserController"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankPath() {
    assertThatThrownBy(() -> new Endpoint(HttpMethod.GET, "  ", "UserController"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsBlankOwner() {
    assertThatThrownBy(() -> new Endpoint(HttpMethod.GET, "/users", "  "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
