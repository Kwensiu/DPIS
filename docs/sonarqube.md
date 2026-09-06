# SonarQube

DPIS uses the official SonarScanner GitHub Action for an optional
static-analysis job. Gradle remains responsible for compilation, unit tests,
and JaCoCo coverage generation. The analysis is advisory for now: it does not
wait for or enforce a SonarQube quality gate, and it does not change GitHub
branch protection.

## Enable the GitHub job

Create a SonarQube Cloud project (or point the configuration at a compatible
SonarQube Server) and add these repository settings:

- Repository variable `SONAR_ENABLED=true`
- Repository secret `SONAR_TOKEN`
- Repository variable `SONAR_PROJECT_KEY`
- Repository variable `SONAR_ORGANIZATION` when using SonarQube Cloud
- Repository variable `SONAR_HOST_URL` when using a server other than the
  default `https://sonarcloud.io`

The job is intentionally disabled until `SONAR_ENABLED` is set. This keeps a
fresh checkout's existing build, test, and lint checks independent from the
external SonarQube project setup.

## Local analysis

From the repository root, run the tests and coverage report first:

```bash
./gradlew :app:testAllDebugUnitTests :app:jacocoModernDebugUnitTestReport
```

Then install the official `sonar-scanner` CLI and run it with the project key,
organization, host URL, and token configured in your environment. The checked
in `sonar-project.properties` supplies the source, binary, test, and coverage
paths shared by local and CI analysis.

The report analyzes Kotlin and Java sources. Native C++ sources and generated
resources are excluded for this first integration. Modern Debug is the primary
coverage input; Legacy unit tests still run as a separate Gradle prerequisite
in CI.
