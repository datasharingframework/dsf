# Maven Linter Quick Start

## Enable (pom.xml)
```xml
<plugin>
  <groupId>dev.dsf</groupId>
  <artifactId>dsf-maven-plugin</artifactId>
  <executions>
    <execution>
      <id>lint-plugin</id>
      <goals>
        <goal>lint</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

## Run
- `mvn verify`
- `mvn deploy`
- `mvn install`
- `mvn dsf-maven-plugin:lint`

## Parameters
- Skip: `-Ddsf.lint.skip=true`
- Do not fail build: `-Ddsf.lint.failOnErrors=false`
- HTML Report: `-Ddsf.lint.html=true`
- JSON Report: `-Ddsf.lint.json=true`
- Verbose Logs: `-Ddsf.lint.verbose=true`
- Report path: `-Ddsf.lint.reportPath=target/dsf-linter-report`
- Exclusion rules: `-Ddsf.lint.exclusionsFile=path/to/dsf-linter-exclusions.json`

## Excluding Issues

Users often encounter known, intentional, or external findings that clutter reports and make triage harder.
The exclusion system lets you suppress specific lint items from HTML and JSON reports without touching the plugin source.

### Option A — Auto-discovery (recommended)

Place a file named `dsf-linter-exclusions.json` in your project root. The plugin picks it up automatically — no extra configuration needed.

### Option B — Explicit file

Pass the path via the Maven property:

```bash
mvn verify -Ddsf.lint.exclusionsFile=path/to/my-exclusions.json
```

Or configure it permanently in `pom.xml`:

```xml
<plugin>
  <groupId>dev.dsf</groupId>
  <artifactId>dsf-maven-plugin</artifactId>
  <configuration>
    <exclusionsFile>${project.basedir}/dsf-linter-exclusions.json</exclusionsFile>
  </configuration>
  <executions>
    <execution>
      <id>lint-plugin</id>
      <goals><goal>lint</goal></goals>
    </execution>
  </executions>
</plugin>
```

### Exclusion file format

```json
{
  "affectsExitStatus": false,
  "rules": [
    { "type": "BPMN_PROCESS_HISTORY_TIME_TO_LIVE_MISSING" },
    { "severity": "WARN", "file": "update-allow-list.bpmn" },
    { "messageContains": "optional field" }
  ]
}
```

**Rule fields** (AND-combined within a rule; multiple rules are OR-combined):

| Field | Match | Example |
|---|---|---|
| `type` | Exact, case-insensitive `LintingType` name | `"BPMN_PROCESS_HISTORY_TIME_TO_LIVE_MISSING"` |
| `severity` | Exact, case-insensitive severity | `"WARN"`, `"ERROR"`, `"INFO"` |
| `file` | Case-insensitive substring of the file name | `"update-allow-list"` |
| `messageContains` | Case-insensitive substring of the description | `"optional field"` |

**`affectsExitStatus`:**

| Value | Behaviour |
|---|---|
| `false` *(default)* | Excluded items are fully suppressed — hidden from reports **and** not counted towards build failure |
| `true` | Excluded items are hidden from reports, but their error count **still** causes a build failure |
