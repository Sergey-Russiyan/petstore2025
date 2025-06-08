package com.petstore.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Configuration Manager using Singleton pattern Manages environment-specific properties */
public class ConfigurationManager {

  private static final Logger log = LoggerFactory.getLogger(ConfigurationManager.class);
  private static ConfigurationManager instance;
  private Properties properties;
  @Getter private final String environment;

  private ConfigurationManager() {
    // Read from system property ENV (matching your workflow: -DENV=staging)
    this.environment = System.getProperty("ENV", "dev"); // Default to dev if not specified
    loadProperties();
  }

  public static ConfigurationManager getInstance() {
    if (instance != null) {
      return instance;
    }
    synchronized (ConfigurationManager.class) {
      if (instance == null) {
        instance = new ConfigurationManager();
      }
    }
    return instance;
  }

  private void loadProperties() {
    properties = new Properties();
    String fileName = environment + ".properties";

    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
      if (inputStream != null) {
        properties.load(inputStream);
        log.info("Loaded configuration from: {} for environment: {}", fileName, environment);
      } else {
        log.error("Configuration file not found: {}", fileName);
        // Fallback to default config
        loadDefaultProperties();
      }
    } catch (IOException e) {
      log.error("Error loading configuration: {}", e.getMessage());
      loadDefaultProperties();
    }
  }

  private void loadDefaultProperties() {
    try (InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("dev.properties")) {
      if (inputStream != null) {
        properties.load(inputStream);
        log.info("Loaded default configuration (dev.properties)");
      } else {
        log.error("Default configuration file (dev.properties) not found");
        setHardcodedDefaults();
      }
    } catch (IOException e) {
      log.error("Error loading default configuration: {}", e.getMessage());
      setHardcodedDefaults();
    }
  }

  private void setHardcodedDefaults() {
    log.warn("Using hardcoded default values");
    properties.setProperty("base.url", "https://petstore.swagger.io/v2");
    properties.setProperty("request.timeout", "30000");
    properties.setProperty("connection.timeout", "10000");
    properties.setProperty("log.requests", "true");
    properties.setProperty("log.responses", "true");
  }

  public String getBaseUrl() {
    return properties.getProperty("base.url", "https://petstore.swagger.io/v2");
  }

  public boolean isRequestLoggingEnabled() {
    return Boolean.parseBoolean(properties.getProperty("log.requests", "true"));
  }

  public boolean isResponseLoggingEnabled() {
    return Boolean.parseBoolean(properties.getProperty("log.responses", "true"));
  }
}
