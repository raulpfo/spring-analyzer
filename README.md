# 🔎 spring-analyzer

**Descubre cómo hablan entre sí tus microservicios Spring Boot — sin instrumentar nada.**

`spring-analyzer` es una CLI que clona un conjunto de repositorios, analiza su código Java/Spring estáticamente y genera un **informe HTML interactivo** con el grafo de dependencias real entre servicios: quién expone qué endpoint, quién lo consume, qué queda huérfano y qué versiones se han quedado atrás.

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=spring&logoColor=white)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Tests](https://img.shields.io/badge/tests-passing-brightgreen)

---

## ✨ Qué hace

Dado un `repos.yml` con la lista de repositorios de tu organización, `spring-analyzer`:

1. **Clona** todos los repos en paralelo (con barra de progreso), sin dejar rastro en disco al terminar.
2. **Detecta endpoints** REST (`@GetMapping`, `@PostMapping`, `@RequestMapping`...) en cada servicio.
3. **Detecta consumidores** entre servicios vía Feign (`@FeignClient`), `RestTemplate` y `WebClient`.
4. **Extrae versiones** de Spring Boot, Java y dependencias desde `pom.xml` / `build.gradle`.
5. **Correlaciona todo** en un grafo de dependencias: qué servicio llama a qué endpoint de qué otro servicio.
6. **Genera un reporte HTML** con:
   - 🕸️ El grafo de dependencias visualizado con [Mermaid](https://mermaid.js.org/).
   - 📋 Una tabla de versiones (Spring Boot, Java, dependencias) por servicio.
   - ⚠️ Anomalías detectadas automáticamente: **endpoints huérfanos** (nadie los consume), **consumidores sin destino** (llaman a algo que no existe) y **versiones desactualizadas** (comparadas entre los propios servicios analizados).

Todo el análisis es estático: no hace falta desplegar nada ni tener acceso de red entre los servicios, solo a sus repositorios git.

---

## 📦 Requisitos

- **Java 21**
- **Maven 3.9+**

(Puedes usar [asdf](https://asdf-vm.com/) con el `.tool-versions` del repo para instalar ambos automáticamente.)

## 🛠️ Compilar

Desde la raíz del proyecto:

```bash
mvn clean package
```

Esto compila los 10 módulos y genera el jar ejecutable (un "fat jar" de Spring Boot) en:

```
spring-analyzer-boot/target/spring-analyzer-boot-0.1.0-SNAPSHOT.jar
```

Para verificar que todo está en orden sin generar el jar, `mvn clean test` es suficiente y más rápido.

---

## 🚀 Guía paso a paso: generar tu primer reporte

### 1. Crea un `repos.yml` con los repositorios a analizar

```yaml
repos:
  - url: https://github.com/tu-org/order-service.git
  - url: https://github.com/tu-org/user-service.git
  - url: https://gitlab.com/tu-org/billing-service.git
    branch: develop
```

Cada entrada admite:

| Campo | Obligatorio | Descripción |
|---|---|---|
| `url` | Sí | URL de clonado del repositorio (HTTPS). |
| `branch` | No | Rama a analizar. Si se omite, se usa la rama por defecto del repositorio. |
| `provider` | No | `github` o `gitlab`. Si se omite, se infiere del *host* de la `url` (`github.com` / `gitlab.com`). Es **obligatorio** especificarlo explícitamente si la URL no permite inferirlo (por ejemplo, un GitHub/GitLab autoalojado con dominio propio). |

### 1.1. (Opcional) Anotaciones custom para frameworks propios

Si tu organización tiene un framework interno construido encima de Spring que envuelve `@RestController`/`@GetMapping`/`@FeignClient` con anotaciones propias, puedes declararlas de forma global en `repos.yml` para que también se detecten (además de las estándar de Spring, que siguen funcionando igual):

```yaml
customAnnotations:
  controllers: [com.acme.fwk.MiController]        # equivalente a @RestController/@Controller
  mappings:
    GET: [com.acme.fwk.MiGet]
    POST: [com.acme.fwk.MiPost]
  consumers: [com.acme.fwk.MiFeignClient]          # equivalente a @FeignClient

repos:
  - url: https://github.com/tu-org/order-service.git
```

Los nombres admiten FQN o nombre simple (se comparan por nombre simple de la anotación, igual que las estándar). Los verbos válidos en `mappings` son `GET`, `POST`, `PUT`, `DELETE` y `REQUEST` (equivalente a `@RequestMapping` sin verbo explícito).

### 2. (Opcional) Configura credenciales si algún repo es privado

`spring-analyzer` nunca lee tokens desde `repos.yml` en texto plano. La resolución de credenciales sigue esta prioridad, por proveedor (`github`/`gitlab`):

1. Flag explícito: `--github-token` / `--gitlab-token`.
2. Variable de entorno indicada con `--token-env <NOMBRE>`.
3. Variable de entorno por defecto del proveedor: `GITHUB_TOKEN` / `GITLAB_TOKEN`.

Si ninguna aplica, el repo se clona sin autenticación (válido para repos públicos).

### 3. Ejecuta el análisis

```bash
java -jar spring-analyzer-boot/target/spring-analyzer-boot-0.1.0-SNAPSHOT.jar \
  -c repos.yml \
  -o report.html
```

Verás una barra de progreso por repositorio mientras se clonan y analizan en paralelo, y al final un resumen:

```
Analisis completado: 3 repositorio(s), 3 analizado(s), 0 no soportado(s), 0 fallido(s). Reporte generado en report.html
```

### 4. Abre `report.html` en tu navegador

Ahí encontrarás el grafo de dependencias, la tabla de versiones y las anomalías detectadas.

---

## ⚙️ Referencia de flags

```
Usage: spring-analyzer [-hvV] [--dry-run] [--keep-temp-dirs] -c=<configPath>
                       [--format=<format>] [--github-token=<githubToken>]
                       [--gitlab-token=<gitlabToken>] [-o=<outputPath>]
                       [--threads=<threads>] [--token-env=<tokenEnv>]
```

| Flag | Descripción |
|---|---|
| `-c, --config=<path>` | **Obligatorio.** Ruta al `repos.yml`. |
| `-o, --output=<path>` | Ruta del reporte generado. Por defecto `report.html`. |
| `--format=<HTML\|MD>` | Formato del reporte. Por defecto `HTML`. ⚠️ Ver [limitaciones](#-limitaciones-actuales). |
| `--github-token=<token>` | Token para clonar repos privados de GitHub. |
| `--gitlab-token=<token>` | Token para clonar repos privados de GitLab. |
| `--token-env=<NOMBRE>` | Nombre de una variable de entorno alternativa que contiene el token. |
| `--threads=<n>` | Nº de hilos concurrentes para clonar y analizar. Por defecto, los procesadores disponibles. |
| `-v, --verbose` | Muestra la configuración resuelta antes de ejecutar. |
| `--dry-run` | Muestra la configuración resuelta y termina sin clonar ni analizar nada. |
| `--keep-temp-dirs` | No borra los directorios temporales de los repos clonados al terminar (útil para depurar). |
| `-h, --help` | Ayuda. |
| `-V, --version` | Versión. |

---

## 🧩 Qué sabe detectar hoy

| Categoría | Soportado |
|---|---|
| Build tool | Maven (`pom.xml`) y Gradle (`build.gradle`, `build.gradle.kts`) en la **raíz** del repositorio |
| Endpoints | `@RestController`/`@Controller` con `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@RequestMapping` |
| Consumidores | `@FeignClient`, `RestTemplate` (`getForObject`, `postForObject`, `put`, `delete`, `exchange`...), `WebClient` (API fluida) |
| Versiones | Spring Boot, Java y dependencias declaradas en `pom.xml`/`build.gradle` |
| Sintaxis Java | Hasta Java 21 (pattern matching, records, sealed classes, etc.) |

## ⚠️ Limitaciones actuales

- **Solo se genera reporte HTML.** `--format md` está expuesto en la CLI pero **no implementado**: falla explícitamente con un error claro en vez de generar HTML disfrazado de Markdown.
- Solo se reconocen repositorios con el fichero de build (`pom.xml`/`build.gradle`) **en la raíz**. Monorepos o proyectos multi-módulo con el build en un subdirectorio se marcan como "no soportado", no como error.
- Solo se analiza el lenguaje Java/Spring; no hay analizadores para otros stacks (aunque la arquitectura interna (`AnalyzerRegistry`) está pensada para añadirlos).
- Las "versiones desactualizadas" se calculan comparando los propios servicios analizados entre sí, no contra la última versión real publicada en Maven Central.

---

## 🏗️ Arquitectura

Proyecto Maven multi-módulo con arquitectura por capas:

```
domain  ←  application (casos de uso)  ←  commands (CLI picocli)  →  boot (entry point)
```

Más los módulos horizontales:

| Módulo | Responsabilidad |
|---|---|
| `domain` | Entidades y puertos puros (`repos.yml`, credenciales, casos de uso) |
| `core` | Utilidades compartidas: parseo de configuración, motor de correlación y grafo de dependencias |
| `scm` | Clonado de repositorios (JGit) y resolución de credenciales por proveedor |
| `analyzers` | Analizadores estáticos basados en JavaParser: endpoints, consumidores, versiones |
| `reporter` | Generación del reporte HTML (Thymeleaf + Mermaid) |
| `ui` | Barras de progreso de terminal |
| `application` | Orquestación real del caso de uso de análisis, conectando todos los colaboradores |
| `commands` | Comando CLI (picocli), adaptador fino que delega en `application` |
| `boot` | Punto de entrada Spring Boot |

## 🧪 Tests

```bash
mvn clean test
```

Corre la batería completa (unitarios + un test de integración end-to-end que ejecuta el flujo real -CLI → clonado → análisis → grafo → reporte- contra repositorios de fixture locales, sin red).
