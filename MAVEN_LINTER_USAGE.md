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
