# spring-analyzer

Analizador estático para arquitecturas de microservicios Spring Boot. CLI (picocli) multi-módulo Maven: dado un `repos.yml`, clona varios repos, los analiza (endpoints, consumidores, dead code, propiedades) y genera un reporte.

Módulos: `domain` (entidades/puertos puros) ← `application` (casos de uso) ← `commands` (CLI picocli) → `boot` (entry point). `core` (utilidades compartidas, parseo de config), `scm` (clonado/API GitHub-GitLab), `analyzers` (JavaParser), `reporter` (Thymeleaf), `ui` (spinners/progreso de terminal).

## Build y tests

```
mvn clean test   # desde la raíz, compila y testea todos los módulos
```

Tests: JUnit 5 + AssertJ + Mockito (via `spring-boot-starter-test`, ya heredado por todos los módulos en el pom raíz, no hace falta añadir dependencias de test).

## Plan de desarrollo

12 hitos atómicos, cada uno es un issue de GitHub (#5–#16) que se implementa en su propia rama y PR. Progreso:

- [x] #5 CLI: interfaz picocli completa con todos los flags
- [x] #6 Scaffolding: corregir estructura de paquetes en ui/application
- [x] #7 Dominio: modelo de repos.yml y resolución segura de credenciales
- [x] #8 SCM: clonado real de repositorios vía JGit
- [x] #9 Core: arquitectura de analizadores extensible por lenguaje
- [x] #10 Analyzers: detección de endpoints Java/Spring
- [x] #11 Analyzers: detección de consumidores (Feign/RestTemplate/WebClient)
- [x] #12 Analyzers: extracción de versiones y dependencias por servicio
- [x] #13 Core: motor de correlación y construcción del grafo de dependencias
- [x] #14 Reporter: reporte HTML con grafo visual de dependencias
- [x] #15 Application: orquestación real del caso de uso de análisis
- [x] #16 Test de integración end-to-end del flujo completo

Actualiza esta lista (marca `[x]`) cada vez que un PR de un hito se mergea a `main`.

## Flujo de trabajo por hito (issue → PR → merge)

El repo usa la extensión `gh sherpa` (InditexTech) para gestionar ramas y PRs a partir de issues de GitHub. Para desarrollar el hito del issue `<N>`:

1. **Rama**: `gh sherpa create-branch -i <N> --branch-type feature -y` (usa `bugfix` en vez de `feature` si el issue es una corrección, p.ej. scaffolding). Sherpa no detecta el tipo por las labels por defecto de GitHub (`enhancement`/`bug`), así que hay que forzarlo con `--branch-type`.
2. **Implementar** el alcance descrito en el issue, **con tests unitarios para cada pieza nueva** (no dejar lógica sin cubrir).
3. **Verificar** con `mvn clean test` en la raíz: build limpio y todos los tests en verde antes de commitear.
4. **Commit**: mensajes descriptivos en español, **sin línea `Co-Authored-By`**. Antes de hacer `git add`, revisar `git status` y excluir cambios ajenos al issue (p.ej. `.idea/*` que el IDE modifica solo).
5. `git push origin <rama>` y `gh sherpa create-pr -i <N> --branch-type feature -y` (crea el PR en draft, vinculado al issue vía "Closes #N").
6. **Rellenar la descripción del PR** con `gh pr edit <PR> --body-file ...`: qué cambia y por qué, qué queda explícitamente fuera de alcance (y en qué issue futuro se cubre), y resumen de los tests añadidos.
7. **Revisión de código antes de mergear**:
   - Diffs con lógica real (parsing, validaciones, ramas de error, algoritmos): lanzar el proceso de revisión de 8 ángulos en paralelo vía subagentes (`Explore`) — 3 de corrección (line-by-line, removed-behavior, cross-file), y 5 de calidad (reuse, simplification, efficiency, altitude, conventions/CLAUDE.md). Cada agente recibe el diff completo (`gh pr diff <N>`) y devuelve hallazgos con `file`/`line`/`summary`/`failure_scenario`. Verificar cada hallazgo superviviente (confirmar o refutar con evidencia concreta, no de oídas) antes de actuar; corregir lo real y de bajo riesgo, descartar el resto explicando por qué.
   - Refactors puramente mecánicos (renombrados, `git mv`, cambios de paquete sin lógica nueva): basta una verificación directa — grep de referencias residuales al nombre/ruta antiguos, y `mvn clean test` — sin lanzar los 8 agentes.
8. **Mergear**: `gh pr ready <N>` (si estaba en draft) y `gh pr merge <N> --squash --subject "<título> (#<N>)"` (squash, consistente con el histórico del repo).
9. **Sincronizar** `main` local: `git checkout main && git pull origin main`.
10. Marcar el hito como hecho en la lista de arriba.

## Convenciones

- **Estructura de paquetes**: código fuente siempre en rutas anidadas reales `spring-analyzer-<modulo>/src/main/java/io/github/springanalyzer/<paquete>/...` (nunca una carpeta literal con puntos tipo `io.github.springanalyzer.foo/`), y el `package` declarado en cada clase debe coincidir con el módulo real donde vive.
- **Credenciales**: nunca en texto plano en `repos.yml`. Usar `CredentialResolver` (domain): prioridad flag CLI (`--github-token`/`--gitlab-token`) > `--token-env` > variable de entorno por defecto del provider (`GITHUB_TOKEN`/`GITLAB_TOKEN`).
- **Commits**: sin línea `Co-Authored-By` (el usuario lo rechazó explícitamente).
- **PRs**: se cierran con squash merge; las ramas de feature no se borran tras el merge (así ha sido en el histórico del repo).
