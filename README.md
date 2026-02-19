# Log4j2 Properties Validator

Validates `log4j2.properties` using **Log4j2 2.25.3**. Reports invalid entries based on Log4j2’s own parsing and schema, not on hardcoded rules.

## What it does (strict mode)

1. **PropertiesConfigurationBuilder**  
   Parses the file via Log4j2’s `PropertiesConfigurationBuilder`, isolating parsing from initialization. Invalid syntax throws `ConfigurationException` directly.

2. **StatusLogger at Level.ALL**  
   Captures all internal Log4j2 messages. Fails on WARN, ERROR, FATAL.

3. **Checks property keys**  
   Log4j2 only allows these top-level prefixes:
   - `appenders`
   - `loggers`
   - `appender.`
   - `logger.`
   - `rootLogger.`  
   Any other key (e.g. `category.*` from Log4j 1.x) is reported as invalid.

## Requirements

- Java 11+
- Maven 3.x

## Build

```bash
mvn clean package
```

## Run

### Standalone JAR (recommended – single file, no lib folder)

```bash
java -jar target/log4j2-validator-1.0.0-standalone.jar /path/to/log4j2.properties
```

Example from `repository/conf`:

```bash
java -jar log4j2-validator/target/log4j2-validator-1.0.0-standalone.jar log4j2.properties
```

For other packs: copy `log4j2-validator-1.0.0-standalone.jar` anywhere and run:

```bash
java -jar log4j2-validator-1.0.0-standalone.jar /path/to/log4j2.properties
```

### Non-standalone (requires lib folder)

```bash
java -cp "target/log4j2-validator-1.0.0.jar:target/lib/*" org.wso2.log4j2.validator.Log4j2Validator /path/to/log4j2.properties
```

## Output

- **Log4j2 StatusLogger (WARN/ERROR)**  
  Messages emitted by Log4j2 while loading the config (e.g. missing appender type, invalid plugin).

- **Likely offending syntax**  
  When the load fails, the validator scans for property keys that use invalid Log4j2 prefixes. Valid prefixes are: `appenders`, `loggers`, `appender.`, `logger.`, `rootLogger.`. Anything else (e.g. `category.*` from Log4j 1.x) causes "No type attribute provided for component" and is reported with the exact line number.

- **Exit code**  
  `0` = no issues; `1` = at least one issue found.

## System properties

- `carbon.home` – If not set, the validator sets it to the directory of the config file so `${sys:carbon.home}` in the properties can resolve.
