# Log4j2 Properties Validator

Validates `log4j2.properties` using **Log4j2 2.25.3**. Reports invalid entries based on Log4j2’s own parsing and schema, not on hardcoded rules.

## What it does (strict mode)

1. **PropertiesConfigurationBuilder**  
   Parses the file via Log4j2’s `PropertiesConfigurationBuilder`, isolating parsing from initialization. Invalid syntax throws `ConfigurationException` directly.

2. **Checks property keys**  
   Log4j2 only allows these top-level prefixes:
   - `appenders`
   - `loggers`
   - `appender.`
   - `logger.`
   - `rootLogger.`  
   Any other key (e.g. `category.*` from Log4j 1.x) is reported as invalid.

3. **Find invalid syntax references**
   Extract valid/known configurations in the same order Log4j does and checks what properties remain after extraction which are invalid.

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
java -jar path/to/log4j2-validator-1.0.0-standalone.jar /path/to/log4j2.properties
```

For any pack: copy `log4j2-validator-1.0.0-standalone.jar` to home and run:

```bash
java -jar log4j2-validator-1.0.0-standalone.jar /path/to/log4j2.properties
```

### Non-standalone (requires lib folder)

```bash
java -cp "target/log4j2-validator-1.0.0.jar:target/lib/*" org.wso2.log4j2.validator.Log4j2Validator /path/to/log4j2.properties
```

## Output

- **Likely offending syntax**  
  When the load fails, the validator scans for property keys that use invalid Log4j2 prefixes. Valid prefixes are: `appenders`, `loggers`, `appender.`, `logger.`, `rootLogger.`. Anything else (e.g. `category.*` from Log4j 1.x) causes "No type attribute provided for component" and is reported with the exact line number.
