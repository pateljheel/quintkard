package io.quintkard.quintkardapp.db;

import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class FlywayMigrationRunner {

    private FlywayMigrationRunner() {
    }

    public static void main(String[] args) {
        Properties applicationProperties = loadApplicationProperties();

        String url = resolveValue("flywayUrl", "FLYWAY_URL", "spring.datasource.url", applicationProperties);
        String user = resolveValue("flywayUser", "FLYWAY_USER", "spring.datasource.username", applicationProperties);
        String password = resolveValue("flywayPassword", "FLYWAY_PASSWORD", "spring.datasource.password", applicationProperties);

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .load();

        flyway.migrate();
    }

    private static Properties loadApplicationProperties() {
        Properties properties = new Properties();
        try (InputStream inputStream = FlywayMigrationRunner.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new IllegalStateException("application.properties not found on the classpath");
            }
            properties.load(inputStream);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load application.properties", exception);
        }
    }

    private static String resolveValue(
            String systemPropertyKey,
            String environmentVariableKey,
            String applicationPropertyKey,
            Properties applicationProperties
    ) {
        String value = System.getProperty(systemPropertyKey);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = System.getenv(environmentVariableKey);
        if (value != null && !value.isBlank()) {
            return value;
        }

        value = applicationProperties.getProperty(applicationPropertyKey);
        if (value != null && !value.isBlank()) {
            return value;
        }

        throw new IllegalStateException("Missing Flyway configuration value for " + applicationPropertyKey);
    }
}
