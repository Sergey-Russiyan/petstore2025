package com.petstore.tests;

import static com.petstore.tests.constants.TestConstants.Headers.CONTENT_TYPE;
import static com.petstore.tests.constants.TestConstants.Headers.DATE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.petstore.client.StoreApiClient;
import com.petstore.config.ConfigurationManager;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.asserts.SoftAssert;

/**
 * Base Test Class - Template Method pattern Provides common setup and utilities for all test
 * classes
 */
public abstract class BaseTest {

  private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

  protected StoreApiClient storeApiClient;
  protected ConfigurationManager config;

  @BeforeSuite
  public void setupClass() {
    // Set up single global filter to prevent duplicates
    RestAssured.replaceFiltersWith(new AllureRestAssured());
  }

  @BeforeClass
  public void baseSetUp() {
    config = ConfigurationManager.getInstance();
    storeApiClient = new StoreApiClient();

    logTestEnvironment();
    performAdditionalSetup();
  }

  @BeforeMethod
  public void beforeMethod(Method method) {
    logTestStart(method);
  }

  /** Hook method for subclasses to perform additional setup */
  protected void performAdditionalSetup() {
    // Default implementation - can be overridden by subclasses
  }

  @Step("Environment: {0}, Base URL: {1}")
  private void logTestEnvironment() {
    log.info("TEST ENVIRONMENT: " + config.getEnvironment() + "\nBASE URL: " + config.getBaseUrl());
  }

  @Step("Starting test: {0}")
  private void logTestStart(Method method) {
    log.info("Starting test: " + method.getName());
  }

  /** Store order specific */
  @Step("Add test delay of {milliseconds}ms")
  protected void addTestDelay(int milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Thread interrupted during test delay");
    }
  }

  @Step("Validate performance results - response time: {responseTime}ms")
  protected void validatePerformanceResults(long responseTime) {
    assertTrue(
        responseTime < com.petstore.tests.constants.TestConstants.Performance.MAX_RESPONSE_TIME_MS,
        String.format(
            "Response time should be less than %dms, actual: %dms",
            com.petstore.tests.constants.TestConstants.Performance.MAX_RESPONSE_TIME_MS,
            responseTime));
  }

  @Step("Validate response headers")
  protected void validateCommonResponseHeaders(Response response) {
    assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_OK, "Status code should be 200");

    SoftAssert softAssert = new SoftAssert();
    softAssert.assertNotNull(
        response.getHeader(CONTENT_TYPE), "Content-Type header should be present");
    softAssert.assertTrue(
        response.getHeader(CONTENT_TYPE).contains("application/json"),
        "Content-Type should be application/json");
    softAssert.assertNotNull(response.getHeader(DATE), "Date header should be present");
    softAssert.assertAll();
  }
}
