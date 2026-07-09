package io.github.springanalyzer.analyzers.version;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ServiceVersionAnalyzerTest {

  private final ServiceVersionAnalyzer analyzer = new ServiceVersionAnalyzer();

  @Test
  void parsesSpringBootVersionJavaVersionAndDependenciesFromMavenPom() {
    final String pomXml = """
        <project>
          <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>3.4.0</version>
          </parent>
          <properties>
            <java.version>21</java.version>
          </properties>
          <dependencies>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-openfeign</artifactId>
              <version>4.1.3</version>
            </dependency>
          </dependencies>
        </project>
        """;

    final ServiceVersionInfo info = analyzer.parsePom(pomXml);

    assertThat(info.springBootVersion()).isEqualTo("3.4.0");
    assertThat(info.javaVersion()).isEqualTo("21");
    assertThat(info.dependencies()).extracting(Dependency::groupId, Dependency::artifactId, Dependency::version)
        .containsExactlyInAnyOrder(
            tuple("org.springframework.boot", "spring-boot-starter-web", null),
            tuple("org.springframework.cloud", "spring-cloud-starter-openfeign", "4.1.3"));
  }

  @Test
  void marksSpringBootAndJavaVersionAsUnknownWhenPomHasNoRecognizedMarkers() {
    final String pomXml = """
        <project>
          <groupId>com.example</groupId>
          <artifactId>legacy-service</artifactId>
          <version>1.0.0</version>
        </project>
        """;

    final ServiceVersionInfo info = analyzer.parsePom(pomXml);

    assertThat(info.springBootVersion()).isNull();
    assertThat(info.javaVersion()).isNull();
    assertThat(info.dependencies()).isEmpty();
  }

  @Test
  void fallsBackToSpringBootVersionPropertyWhenThereIsNoParent() {
    final String pomXml = """
        <project>
          <properties>
            <spring-boot.version>3.4.0</spring-boot.version>
          </properties>
        </project>
        """;

    final ServiceVersionInfo info = analyzer.parsePom(pomXml);

    assertThat(info.springBootVersion()).isEqualTo("3.4.0");
  }

  @Test
  void fallsBackToMavenCompilerReleaseWhenJavaVersionPropertyIsMissing() {
    final String pomXml = """
        <project>
          <properties>
            <maven.compiler.release>21</maven.compiler.release>
          </properties>
        </project>
        """;

    final ServiceVersionInfo info = analyzer.parsePom(pomXml);

    assertThat(info.javaVersion()).isEqualTo("21");
  }

  @Test
  void fallsBackToMavenCompilerSourceWhenJavaVersionAndReleasePropertiesAreMissing() {
    final String pomXml = """
        <project>
          <properties>
            <maven.compiler.source>17</maven.compiler.source>
          </properties>
        </project>
        """;

    final ServiceVersionInfo info = analyzer.parsePom(pomXml);

    assertThat(info.javaVersion()).isEqualTo("17");
  }

  @Test
  void ignoresDependenciesDeclaredOnlyInDependencyManagement() {
    final String pomXml = """
        <project>
          <dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>3.4.0</version>
              </dependency>
            </dependencies>
          </dependencyManagement>
          <dependencies>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
              <version>3.4.0</version>
            </dependency>
          </dependencies>
        </project>
        """;

    final ServiceVersionInfo info = analyzer.parsePom(pomXml);

    assertThat(info.dependencies()).extracting(Dependency::artifactId).containsExactly("spring-boot-starter-web");
  }

  @Test
  void skipsMalformedDependencyEntriesWithoutFailing() {
    final String pomXml = """
        <project>
          <dependencies>
            <dependency>
              <artifactId>missing-group-id</artifactId>
            </dependency>
            <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
          </dependencies>
        </project>
        """;

    final ServiceVersionInfo info = analyzer.parsePom(pomXml);

    assertThat(info.dependencies()).extracting(Dependency::artifactId).containsExactly("spring-boot-starter-web");
  }

  @Test
  void returnsUnknownInfoWhenPomXmlIsMalformedInsteadOfFailing() {
    final String malformedXml = "<project><parent><groupId>org.springframework.boot</groupId>";

    final ServiceVersionInfo info = analyzer.parsePom(malformedXml);

    assertThat(info.springBootVersion()).isNull();
    assertThat(info.javaVersion()).isNull();
    assertThat(info.dependencies()).isEmpty();
  }

  @Test
  void parsesSpringBootVersionJavaVersionAndDependenciesFromGradleGroovyDsl() {
    final String buildGradle = """
        plugins {
            id 'org.springframework.boot' version '3.4.0'
            id 'io.spring.dependency-management' version '1.1.6'
            id 'java'
        }

        sourceCompatibility = '21'

        dependencies {
            implementation 'org.springframework.boot:spring-boot-starter-web'
            implementation 'org.springframework.cloud:spring-cloud-starter-openfeign:4.1.3'
            testImplementation 'org.springframework.boot:spring-boot-starter-test'
        }
        """;

    final ServiceVersionInfo info = analyzer.parseGradle(buildGradle);

    assertThat(info.springBootVersion()).isEqualTo("3.4.0");
    assertThat(info.javaVersion()).isEqualTo("21");
    assertThat(info.dependencies()).extracting(Dependency::groupId, Dependency::artifactId, Dependency::version)
        .containsExactlyInAnyOrder(
            tuple("org.springframework.boot", "spring-boot-starter-web", null),
            tuple("org.springframework.cloud", "spring-cloud-starter-openfeign", "4.1.3"),
            tuple("org.springframework.boot", "spring-boot-starter-test", null));
  }

  @Test
  void parsesJavaVersionExpressedAsJavaVersionEnumConstant() {
    final String buildGradle = """
        sourceCompatibility = JavaVersion.VERSION_17
        """;

    final ServiceVersionInfo info = analyzer.parseGradle(buildGradle);

    assertThat(info.javaVersion()).isEqualTo("17");
  }

  @Test
  void marksSpringBootAndJavaVersionAsUnknownWhenGradleFileHasNoRecognizedMarkers() {
    final String buildGradle = """
        plugins {
            id 'java'
        }
        """;

    final ServiceVersionInfo info = analyzer.parseGradle(buildGradle);

    assertThat(info.springBootVersion()).isNull();
    assertThat(info.javaVersion()).isNull();
    assertThat(info.dependencies()).isEmpty();
  }

  @Test
  void skipsGradleDependenciesInUnrecognizedMapStyleFormat() {
    final String buildGradle = """
        dependencies {
            implementation group: 'org.springframework.boot', name: 'spring-boot-starter-web', version: '3.4.0'
            implementation 'org.springframework.cloud:spring-cloud-starter-openfeign:4.1.3'
        }
        """;

    final ServiceVersionInfo info = analyzer.parseGradle(buildGradle);

    assertThat(info.dependencies()).extracting(Dependency::artifactId)
        .containsExactly("spring-cloud-starter-openfeign");
  }

  @Test
  void returnsAllUnknownFieldsWhenGradleFileContentIsUnrecognizableInsteadOfFailing() {
    final String garbledContent = "{{{ this is not valid Groovy at all ] } ) (";

    final ServiceVersionInfo info = analyzer.parseGradle(garbledContent);

    assertThat(info.springBootVersion()).isNull();
    assertThat(info.javaVersion()).isNull();
    assertThat(info.dependencies()).isEmpty();
  }

  @Test
  void analyzeUsesMavenPomWhenPresent(@TempDir final Path repoRoot) throws IOException {
    Files.writeString(repoRoot.resolve("pom.xml"), """
        <project>
          <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>3.4.0</version>
          </parent>
        </project>
        """);

    final ServiceVersionInfo info = analyzer.analyze(repoRoot);

    assertThat(info.springBootVersion()).isEqualTo("3.4.0");
  }

  @Test
  void analyzeUsesGradleBuildWhenNoPomIsPresent(@TempDir final Path repoRoot) throws IOException {
    Files.writeString(repoRoot.resolve("build.gradle"), """
        plugins {
            id 'org.springframework.boot' version '3.4.0'
        }
        """);

    final ServiceVersionInfo info = analyzer.analyze(repoRoot);

    assertThat(info.springBootVersion()).isEqualTo("3.4.0");
  }

  @Test
  void analyzePrefersMavenPomWhenBothBuildFilesArePresent(@TempDir final Path repoRoot) throws IOException {
    Files.writeString(repoRoot.resolve("pom.xml"), """
        <project>
          <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>3.4.0</version>
          </parent>
        </project>
        """);
    Files.writeString(repoRoot.resolve("build.gradle"), """
        plugins {
            id 'org.springframework.boot' version '2.7.0'
        }
        """);

    final ServiceVersionInfo info = analyzer.analyze(repoRoot);

    assertThat(info.springBootVersion()).isEqualTo("3.4.0");
  }

  @Test
  void analyzeReturnsUnknownInfoWhenNoBuildFileIsPresent(@TempDir final Path repoRoot) {
    final ServiceVersionInfo info = analyzer.analyze(repoRoot);

    assertThat(info.springBootVersion()).isNull();
    assertThat(info.javaVersion()).isNull();
    assertThat(info.dependencies()).isEmpty();
  }
}
