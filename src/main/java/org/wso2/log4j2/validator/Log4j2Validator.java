package org.wso2.log4j2.validator;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationBuilder;
import org.apache.logging.log4j.util.PropertiesUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Generic validator for log4j2.properties using Log4j2 2.25.3.
 * Replicates PropertiesConfigurationBuilder's extraction logic (PropertiesUtil.extractSubset)
 * to find properties that remain after processing - these are passed to processRemainingProperties
 * and cause "No type attribute provided for component X" when invalid.
 * No hardcoded checks; invalid syntax is whatever Log4j2 does not recognize.
 */
public class Log4j2Validator {

    private static final String INVALID_HEADER =
            "Invalid syntax: These properties are not recognized by Log4j2. If you are not using them, comment or remove these entries.";

    public static void main(String[] args) {
        System.setProperty("log4j2.statusLoggerLevel", "OFF");
        if (args.length == 0) {
            System.out.println("Usage: java -jar validator.jar <log4j2.properties>");
            System.exit(1);
        }

        File configFile = new File(args[0]);
        if (!configFile.exists()) {
            System.err.println("File not found: " + args[0]);
            System.exit(1);
        }

        validate(configFile);
    }

    private static void validate(File configFile) {
        int exitCode = 0;

        if (System.getProperty("carbon.home") == null) {
            System.setProperty("carbon.home", configFile.getParentFile().getAbsolutePath());
        }

        try {
            Properties props = new Properties();
            try (InputStream is = new FileInputStream(configFile)) {
                props.load(is);
            }
            ConfigurationSource source = new ConfigurationSource(
                    new FileInputStream(configFile), configFile);
            LoggerContext ctx = LoggerContext.getContext(false);

            PropertiesConfigurationBuilder builder = new PropertiesConfigurationBuilder()
                    .setRootProperties(props)
                    .setConfigurationSource(source)
                    .setLoggerContext(ctx);

            builder.build();
        } catch (Exception e) {
            exitCode = 1;
            System.out.println("\n Configuration parse failed (PropertiesConfigurationBuilder):");
            e.printStackTrace();
        }

        // Generic: find properties that remain after Log4j2 extraction (= invalid, cause processRemainingProperties to fail)
        List<String> invalidKeys = findRemainingPropertiesViaExtraction(configFile);
        if (!invalidKeys.isEmpty()) {
            System.out.println("\n" + INVALID_HEADER);
            printGroupedInvalidKeys(invalidKeys);
            exitCode = 1;
        } else {
            System.out.println("\n Existing Configurations are valid.");
        }

        System.exit(exitCode);
    }

    /**
     * Groups invalid keys by "componentType: componentName"
     */
    private static void printGroupedInvalidKeys(List<String> invalidKeys) {
        Map<String, List<String>> groups = new TreeMap<>();
        for (String key : invalidKeys) {
            String[] parts = key.split("\\.", 3);
            String groupKey = parts.length >= 2 ? parts[0] + ": " + parts[1] : key;
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(key);
        }
        for (Map.Entry<String, List<String>> e : groups.entrySet()) {
            System.out.println();
            System.out.println("  " + e.getKey());
            for (String prop : e.getValue()) {
                System.out.println("    " + prop);
            }
        }
    }

    /**
     * Replicates PropertiesConfigurationBuilder.build() extraction sequence using Log4j2's
     * PropertiesUtil.extractSubset. Properties that remain after all extractions are passed to
     * processRemainingProperties and cause "No type attribute provided for component X".
     * Returns those remaining property keys (invalid syntax).
     */
    private static List<String> findRemainingPropertiesViaExtraction(File configFile) {
        Properties props;
        try (InputStream is = new FileInputStream(configFile)) {
            props = new Properties();
            props.load(is);
        } catch (IOException e) {
            return new ArrayList<>();
        }

        Properties rootProperties = new Properties();
        rootProperties.putAll(props);

        PropertiesUtil.extractSubset(rootProperties, "property");
        PropertiesUtil.extractSubset(rootProperties, "script");
        PropertiesUtil.extractSubset(rootProperties, "customLevel");

        String filterProp = rootProperties.getProperty("filters");
        if (filterProp != null) {
            for (String name : filterProp.split(",")) {
                PropertiesUtil.extractSubset(rootProperties, "filter." + name.trim());
            }
        } else {
            PropertiesUtil.extractSubset(rootProperties, "filter");
        }

        String appenderProp = rootProperties.getProperty("appenders");
        if (appenderProp != null) {
            for (String name : appenderProp.split(",")) {
                PropertiesUtil.extractSubset(rootProperties, "appender." + name.trim());
            }
        } else {
            PropertiesUtil.extractSubset(rootProperties, "appender");
        }

        String loggerProp = rootProperties.getProperty("loggers");
        if (loggerProp != null) {
            for (String name : loggerProp.split(",")) {
                String n = name.trim();
                if (!"root".equalsIgnoreCase(n)) {
                    PropertiesUtil.extractSubset(rootProperties, "logger." + n);
                }
            }
        } else {
            PropertiesUtil.extractSubset(rootProperties, "logger");
        }

        PropertiesUtil.extractSubset(rootProperties, "rootLogger");
        if (rootProperties.getProperty("rootLogger") != null) {
            rootProperties.remove("rootLogger");
        }

        List<String> remaining = new ArrayList<>(new TreeSet<>(rootProperties.stringPropertyNames()));
        remaining.removeIf(k -> !k.contains("."));
        return remaining;
    }
}
