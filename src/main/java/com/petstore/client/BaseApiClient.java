package com.petstore.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.petstore.config.ConfigurationManager;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import lombok.Getter;

/**
 * Base API Client using Template Method pattern Provides common configuration for all API clients
 */
public abstract class BaseApiClient {
  protected ConfigurationManager config;

  /** -- GETTER -- Get request specification */
  @Getter protected RequestSpecification requestSpec;

  /** -- GETTER -- Get response specification */
  @Getter protected ResponseSpecification responseSpec;

  public BaseApiClient() {
    this.config = ConfigurationManager.getInstance();
    setupJacksonConfiguration();
    setupRequestSpecification();
    setupResponseSpecification();
  }

  /** Configure Jackson to handle date serialization properly */
  private void setupJacksonConfiguration() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Configure RestAssured to use this ObjectMapper

    RestAssured.config =
        RestAssuredConfig.config()
            .objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory((cls, charset) -> objectMapper));
  }

  /** Template method for setting up request specification */
  private void setupRequestSpecification() {
    RequestSpecBuilder builder =
        new RequestSpecBuilder().setBaseUri(config.getBaseUrl()).setContentType(ContentType.JSON);
    //            .addFilter(new Allure());

    // Add logging based on configuration
    if (config.isRequestLoggingEnabled()) {
      builder.log(LogDetail.ALL);
    }

    // Add custom headers if needed
    addCustomHeaders(builder);

    this.requestSpec = builder.build();

    // Set global REST Assured configuration
    RestAssured.requestSpecification = requestSpec;
  }

  /** Template method for setting up response specification */
  private void setupResponseSpecification() {
    ResponseSpecBuilder builder = new ResponseSpecBuilder();

    // Add logging based on configuration
    if (config.isResponseLoggingEnabled()) {
      builder.log(LogDetail.ALL);
    }

    // Add custom response validations if needed
    addCustomResponseValidations();

    this.responseSpec = builder.build();
  }

  /** Hook method for subclasses to add custom headers */
  protected void addCustomHeaders(RequestSpecBuilder builder) {
    // Default implementation - can be overridden by subclasses
    builder.addHeader("Accept", "application/json");
  }

  /** Hook method for subclasses to add custom response validations */
  protected void addCustomResponseValidations() {
    // Default implementation - can be overridden by subclasses
  }

  /** Get base URL from configuration */
  protected String getBaseUrl() {
    return config.getBaseUrl();
  }
}
