package com.petstore.tests;

import static com.petstore.tests.constants.TestConstants.Headers.*;
import static com.petstore.tests.constants.TestConstants.Performance.MAX_RESPONSE_TIME_MS;
import static org.testng.Assert.*;

import com.petstore.dto.ErrorResponse;
import com.petstore.dto.Order;
import com.petstore.dto.OrderStatus;
import com.petstore.tests.constants.TestConstants;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import java.net.HttpURLConnection;
import java.time.OffsetDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.asserts.SoftAssert;

/** Base Store Test Class - extends BaseTest Provides store-specific helper methods and utilities */
public abstract class BaseStoreTest extends BaseTest {

  private static final Logger log = LoggerFactory.getLogger(BaseStoreTest.class);
  protected List<Long> createdOrderIds = new ArrayList<>();
  private static final Set<String> EXPECTED_PET_STATUSES =
      new HashSet<>(Arrays.asList("available", "pending", "sold"));

  protected Order createValidOrder() {
    return Order.builder()
        .petId(1L)
        .quantity(1)
        .shipDate(OffsetDateTime.now())
        .status(OrderStatus.PLACED)
        .complete(false)
        .build();
  }

  protected Order createValidOrderWithCustomFields(
      Long petId, Integer quantity, OrderStatus status) {
    return Order.builder()
        .petId(petId)
        .quantity(quantity)
        .shipDate(OffsetDateTime.now())
        .status(status)
        .complete(false)
        .build();
  }

  protected void assertOrderMatch(Order expected, Order actual, String message) {
    assertNotNull(actual, message + " - Order should not be null");
    if (expected.getId() != null && expected.getId() != 0L) {
      assertEquals(actual.getId(), expected.getId(), message + " - Order ID should match");
    } else {
      assertNotNull(actual.getId(), message + " - Order ID should be assigned");
    }
    assertEquals(actual.getPetId(), expected.getPetId(), message + " - Pet ID should match");
    assertEquals(
        actual.getQuantity(), expected.getQuantity(), message + " - Quantity should match");
    if (expected.getStatus() != null) {
      assertEquals(actual.getStatus(), expected.getStatus(), message + " - Status should match");
    }
    if (expected.getComplete() != null) {
      assertEquals(
          actual.getComplete(), expected.getComplete(), message + " - Complete flag should match");
    }
  }

  protected void assertErrorResponse(Response response) {
    assertEquals(
        response.getStatusCode(),
        HttpURLConnection.HTTP_NOT_FOUND,
        "Status code should be " + HttpURLConnection.HTTP_NOT_FOUND);

    if (response.getBody().asString().contains("code")) {
      ErrorResponse errorResponse = response.as(ErrorResponse.class);
      assertNotNull(errorResponse, "Error response should not be null");
      assertEquals(
          errorResponse.getType(), TestConstants.ErrorTypes.ERROR, "Error type should match");
      assertEquals(
          errorResponse.getMessage(),
          TestConstants.ErrorMessages.ORDER_NOT_FOUND,
          "Error message should match");
    }
  }

  protected Long placeOrderAndTrack(Order order) {
    Response response = storeApiClient.placeOrder(order);
    assertEquals(
        response.getStatusCode(), HttpURLConnection.HTTP_OK, "Order creation should succeed");

    Order createdOrder = response.as(Order.class);
    Long orderId = createdOrder.getId();
    createdOrderIds.add(orderId);

    return orderId;
  }

  // ==================== CLEANUP ====================

  @AfterMethod
  protected void cleanupCreatedOrders() {
    // Clean up any orders created during tests
    for (Long orderId : createdOrderIds) {
      try {
        storeApiClient.deleteOrder(orderId);
      } catch (Exception e) {
        // Log but don't fail test cleanup
        log.info("Failed to cleanup order ID: " + orderId);
      }
    }
    createdOrderIds.clear();
  }

  // ==================== UTILITY METHODS ====================

  @Step("Validate inventory structure and values")
  private void assertInventoryValid(Map<String, Integer> inventory) {
    log.debug("Validating inventory structure and values");

    assertNotNull(inventory, "Inventory should not be null");
    assertFalse(inventory.isEmpty(), "Inventory should not be empty");

    SoftAssert softAssert = new SoftAssert();
    inventory.forEach(
        (status, count) -> {
          softAssert.assertNotNull(count, "Count should not be null");
          softAssert.assertTrue(count >= 0, "Count should be non-negative");
          softAssert.assertFalse(status.trim().isEmpty(), "Status name should not be empty");
        });
    softAssert.assertAll();

    log.debug("Inventory validation completed for {} statuses", inventory.size());
  }

  @Step("Create test order")
  protected Order createTestOrder() {
    return Order.builder().petId(1L).quantity(1).status(OrderStatus.PLACED).build();
  }

  @Step("Place test order")
  protected Response placeTestOrder(Order order) {
    log.info("Placing test order: {}", order);
    Response response = storeApiClient.placeOrder(order);
    log.info("Order placement response status: {}", response.getStatusCode());
    return response;
  }

  @Step("Validate inventory changes")
  protected void validateInventoryChanges(
      Map<String, Integer> initialInventory, Response orderResponse) {
    if (orderResponse.getStatusCode() == HttpURLConnection.HTTP_OK) {
      addOrderProcessingDelay();
      Map<String, Integer> updatedInventory = retrieveInventory();

      int initialAvailable = initialInventory.getOrDefault("available", 0);
      int updatedAvailable = updatedInventory.getOrDefault("available", 0);
      int initialPending = initialInventory.getOrDefault("pending", 0);
      int updatedPending = updatedInventory.getOrDefault("pending", 0);

      assertTrue(
          updatedAvailable <= initialAvailable,
          "Available count should decrease or stay same after order placement");
      assertTrue(
          updatedPending >= initialPending,
          "Pending count should increase or stay same after order placement");

      log.info("Inventory change validation completed successfully");
    } else {
      fail("Order placement failed, cannot test inventory changes");
    }
  }

  @Step("Add order processing delay")
  private void addOrderProcessingDelay() {
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Thread interrupted during order processing delay");
    }
  }

  @Step("Validate inventory consistency")
  protected void validateConsistency(
      Map<String, Integer> inventory1, Map<String, Integer> inventory2) {
    assertInventoryValid(inventory1);
    assertInventoryValid(inventory2);

    SoftAssert softAssert = new SoftAssert();
    inventory1.forEach(
        (status, count) ->
            softAssert.assertTrue(
                count >= 0,
                String.format("Status '%s' count should be non-negative (first call)", status)));

    inventory2.forEach(
        (status, count) ->
            softAssert.assertTrue(
                count >= 0,
                String.format("Status '%s' count should be non-negative (second call)", status)));

    softAssert.assertAll();
    log.info("Consistency validation completed");
  }

  @Step("Validate response headers")
  protected void validateResponseHeaders(Response response) {
    validateCommonResponseHeaders(response);

    SoftAssert softAssert = new SoftAssert();

    String corsOrigin = response.getHeader(CORS_ORIGIN);
    softAssert.assertEquals(corsOrigin, "*", "CORS origin should allow all");

    String corsMethods = response.getHeader(CORS_METHODS);
    softAssert.assertNotNull(corsMethods, "CORS methods header should be present");

    String corsHeaders = response.getHeader(CORS_HEADERS);
    softAssert.assertNotNull(corsHeaders, "CORS headers should be present");

    String serverHeader = response.getHeader(TestConstants.Headers.SERVER);
    softAssert.assertNotNull(serverHeader, "Server header should be present");
    softAssert.assertTrue(serverHeader.contains("Jetty"), "Server should be Jetty");
    softAssert.assertAll();
    log.info("All header validations passed");
  }

  @Step("Retrieve inventory")
  protected Map<String, Integer> retrieveInventory() {
    Map<String, Integer> inventory = storeApiClient.getInventoryAsMap();
    log.info("Retrieved inventory with status types: {}", inventory.keySet());
    return inventory;
  }

  @Step("Validate expected statuses in inventory")
  protected void validateExpectedStatuses(Map<String, Integer> inventory) {
    assertInventoryValid(inventory);

    Set<String> inventoryStatuses = inventory.keySet();
    boolean hasExpectedStatus =
        inventoryStatuses.stream().anyMatch(EXPECTED_PET_STATUSES::contains);

    assertTrue(
        hasExpectedStatus,
        String.format(
            "Inventory should contain at least one expected pet status from %s, but found: %s",
            EXPECTED_PET_STATUSES, inventoryStatuses));

    log.info("Status validation completed");
  }

  @Step("Measure inventory response time")
  protected long measureInventoryResponseTime() {
    long startTime = System.currentTimeMillis();
    Response response = storeApiClient.getInventory();
    long responseTime = System.currentTimeMillis() - startTime;
    log.info(
        "Inventory response completed in {}ms (threshold: {}ms)",
        responseTime,
        MAX_RESPONSE_TIME_MS);
    assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_OK, "Status code should be 200");
    return responseTime;
  }

  @Step("Execute inventory request")
  protected Response executeInventoryRequest() {
    Response response = storeApiClient.getInventory();
    log.debug("Inventory response status: {}", response.getStatusCode());
    return response;
  }

  @Step("Validate inventory response")
  protected void validateInventoryResponse(Response response) {
    assertEquals(response.getStatusCode(), HttpURLConnection.HTTP_OK, "Status code should be 200");

    String contentType = response.getHeader(TestConstants.Headers.CONTENT_TYPE);
    assertTrue(
        contentType.contains(TestConstants.ContentTypes.JSON),
        "Content-Type should be application/json");

    var inventory = response.as(Map.class);
    assertInventoryValid(inventory);
    log.info("Inventory validation completed successfully");
  }

  @Step("Cleanup orders")
  private int cleanupOrders() {
    int failures = 0;
    for (Long orderId : createdOrderIds) {
      try {
        Response deleteResponse = storeApiClient.deleteOrder(orderId);
        if (isSuccessfulCleanup(deleteResponse.getStatusCode())) {
          log.debug("Successfully cleaned up order {}", orderId);
        } else {
          log.warn(
              "Cleanup failed for order {} with status code {}",
              orderId,
              deleteResponse.getStatusCode());
          failures++;
        }
      } catch (Exception e) {
        log.error("Exception during cleanup of order {}: {}", orderId, e.getMessage(), e);
        failures++;
      }
    }
    return failures;
  }

  private boolean isSuccessfulCleanup(int statusCode) {
    return statusCode == HttpURLConnection.HTTP_OK
        || statusCode == HttpURLConnection.HTTP_NOT_FOUND;
  }

  @Step("Track order for cleanup")
  protected void trackOrderForCleanup(Response response) {
    if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
      Order createdOrder = response.as(Order.class);
      if (createdOrder.getId() != null) {
        Long orderId = createdOrder.getId();
        Allure.parameter("orderId", orderId.toString()); // Convert to String for Allure
        createdOrderIds.add(orderId);
        log.debug("Tracking order {} for cleanup", orderId);
      }
    }
  }

  @Step("Handle order validation response for: {description}")
  protected void handleOrderValidationResponse(Response response, String description) {
    if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
      log.warn(
          "✓ API accepted potentially invalid data: {} (Status: {})",
          description,
          response.getStatusCode());
      trackOrderForCleanup(response);
    } else {
      log.info(
          "✗ API properly rejected invalid data: {} (Status: {})",
          description,
          response.getStatusCode());
    }
  }

  @Step("Validate invalid order response for test case: {testCase} ({operation})")
  protected void validateInvalidOrderResponse(
      Response response, String testCase, String operation) {
    assertEquals(
        response.getStatusCode(),
        HttpURLConnection.HTTP_NOT_FOUND,
        String.format(
            "Test case '%s' for %s: Status code should be 404, got %d",
            testCase, operation, response.getStatusCode()));
    log.info("Test case '{}' for {} passed: properly returned 404", testCase, operation);
  }

  @Step("Perform cleanup for created orders")
  protected void performOrderCleanup() {
    log.info("Starting cleanup for {} created orders", createdOrderIds.size());
    int cleanupFailures = cleanupOrders();

    if (cleanupFailures > 0) {
      log.warn(
          "Cleanup completed with {} failures out of {} orders",
          cleanupFailures,
          createdOrderIds.size());
    } else {
      log.info("Cleanup completed successfully for all {} orders", createdOrderIds.size());
    }
  }

  @Step("Execute GET request for non-existent order ID: {nonExistentOrderId}")
  protected Response executeGetNonExistentOrder(Long nonExistentOrderId) {
    Response response = storeApiClient.getOrderById(nonExistentOrderId);
    log.debug("Response status for non-existent order: {}", response.getStatusCode());
    return response;
  }

  @Step("Validate non-existent order response")
  protected void validateNonExistentOrderResponse(Response response) {
    assertErrorResponse(response);
    log.info("Non-existent order test passed: proper error response received");
  }

  @Step("Execute order placement with invalid data: {testCase}")
  protected Response executeOrderPlacementWithInvalidData(Order invalidOrder, String testCase) {
    Response response = storeApiClient.placeOrder(invalidOrder);
    int actualStatusCode = response.getStatusCode();
    log.debug("Response status for invalid order test case '{}': {}", testCase, actualStatusCode);
    return response;
  }

  @Step("Validate invalid data rejection for test case: {testCase}")
  protected void validateInvalidDataRejection(Response response, String testCase) {
    int actualStatusCode = response.getStatusCode();

    try {
      // Primary assertion: Should return client error status (4xx)
      assertTrue(
          actualStatusCode >= 400 && actualStatusCode <= 499,
          String.format(
              "API should reject invalid order data with 4xx status for test case: %s, but got: %d",
              testCase, actualStatusCode));

      // Additional validation: Should not return success status
      assertNotEquals(
          actualStatusCode,
          HttpURLConnection.HTTP_OK,
          String.format("API should not return success status for invalid data: %s", testCase));

      // Verify response has error details (common API practice)
      String responseBody = response.getBody().asString();
      assertFalse(
          responseBody == null || responseBody.trim().isEmpty(),
          String.format("Error response should contain details for test case: %s", testCase));

      log.info("Successfully validated rejection of invalid data for test case: {}", testCase);

    } catch (AssertionError e) {
      handleValidationFailure(response, testCase, actualStatusCode, e);
    } catch (Exception e) {
      handleUnexpectedValidationError(testCase, e);
    }
  }

  @Step("CRITICAL: Validation failure detected for test case: {testCase}")
  private void handleValidationFailure(
      Response response, String testCase, int actualStatusCode, AssertionError e) {
    log.error("VALIDATION FAILURE DETECTED for test case: {}", testCase);
    log.error("Expected: 4xx status code, Actual: {}", actualStatusCode);
    log.error("Response body: {}", response.getBody().asString());

    // Track for cleanup if order was unexpectedly created
    if (actualStatusCode >= 200 && actualStatusCode < 300) {
      trackOrderForCleanup(response);
      log.warn("Tracking unexpectedly created order for cleanup: {}", testCase);
    }

    // Re-throw with enhanced error message
    throw new AssertionError(
        String.format(
            "Critical validation gap detected: API accepted invalid order data for '%s'. "
                + "Expected 4xx status but got %d. This indicates missing API validation. "
                + "Response: %s",
            testCase, actualStatusCode, response.getBody().asString()),
        e);
  }

  @Step("Unexpected error during validation test for case: {testCase}")
  private void handleUnexpectedValidationError(String testCase, Exception e) {
    log.error("Unexpected error during invalid data test for case: {}", testCase, e);
    fail(String.format("Test failed unexpectedly for case '%s': %s", testCase, e.getMessage()));
  }

  @Step("Execute DELETE order request with invalid ID: {invalidOrderId}")
  protected Response executeDeleteOrderWithInvalidId(Long invalidOrderId) {
    Response response = storeApiClient.deleteOrder(invalidOrderId);
    log.debug(
        "Response status for invalid order ID deletion {}: {}",
        invalidOrderId,
        response.getStatusCode());
    return response;
  }

  @Step("Execute GET order request with invalid ID: {invalidOrderId}")
  protected Response executeGetOrderWithInvalidId(Long invalidOrderId) {
    Response response = storeApiClient.getOrderById(invalidOrderId);
    log.debug(
        "Response status for invalid order ID {}: {}", invalidOrderId, response.getStatusCode());
    return response;
  }

  @Step("Create order for lifecycle test")
  protected Long createOrderForLifecycleTest(Order orderRequest) {
    Long orderId = placeOrderAndTrack(orderRequest);
    log.info("Created order {} for lifecycle test", orderId);
    return orderId;
  }

  @Step("Validate order creation with ID: {orderId}")
  protected void validateOrderCreation(Long orderId) {
    assertNotNull(orderId, "Order ID should not be null after successful creation");
    assertTrue(orderId > 0, "Order ID should be a positive number");
  }

  @Step("Retrieve and validate order")
  protected void retrieveAndValidateOrder(Long orderId, Order originalOrder) {
    Response getResponse = storeApiClient.getOrderById(orderId);

    validateOrderRetrieval(getResponse, orderId);
    Order retrievedOrder = getResponse.as(Order.class);
    assertNotNull(retrievedOrder, "Retrieved order should not be null");

    assertOrderMatch(originalOrder, retrievedOrder, "Order retrieval");
    log.info("Successfully retrieved and validated order {}", orderId);
  }

  @Step("Validate order retrieval response for order ID: {orderId}")
  private void validateOrderRetrieval(Response getResponse, Long orderId) {
    assertEquals(
        getResponse.getStatusCode(),
        HttpURLConnection.HTTP_OK,
        String.format(
            "Order retrieval should succeed for order ID: %d, but got status: %d",
            orderId, getResponse.getStatusCode()));

    String responseBody = getResponse.getBody().asString();
    assertFalse(
        responseBody == null || responseBody.trim().isEmpty(),
        "Response body should contain order data");
  }

  @Step("Delete order and validate removal for ID: {orderId}")
  protected void deleteAndValidateOrderRemoval(Long orderId) {
    Response deleteResponse = storeApiClient.deleteOrder(orderId);

    validateOrderDeletion(deleteResponse, orderId);
    log.info("Successfully deleted order {}", orderId);

    verifyOrderActuallyDeleted(orderId);
    createdOrderIds.remove(orderId);
  }

  @Step("Validate order deletion response for order ID: {orderId}")
  private void validateOrderDeletion(Response deleteResponse, Long orderId) {
    assertTrue(
        deleteResponse.getStatusCode() >= 200 && deleteResponse.getStatusCode() < 300,
        String.format(
            "Order deletion should succeed for order ID: %d, but got status: %d",
            orderId, deleteResponse.getStatusCode()));
  }

  @Step("Verify order {orderId} is actually deleted")
  private void verifyOrderActuallyDeleted(Long orderId) {
    Response verifyDeleteResponse = storeApiClient.getOrderById(orderId);

    assertEquals(
        verifyDeleteResponse.getStatusCode(),
        HttpURLConnection.HTTP_NOT_FOUND,
        String.format("Deleted order %d should return 404 when accessed", orderId));

    log.info("Confirmed order {} is properly deleted", orderId);
  }

  @Step("Handle lifecycle test failure for order ID: {orderId}")
  protected void handleLifecycleTestFailure(Long orderId, Exception e) {
    log.error("Order lifecycle test failed for order ID: {}", orderId, e);

    if (orderId != null) {
      cleanupFailedLifecycleOrder(orderId);
    }

    fail(String.format("Order lifecycle test failed: %s", e.getMessage()));
  }

  @Step("Cleanup failed lifecycle order: {orderId}")
  private void cleanupFailedLifecycleOrder(Long orderId) {
    try {
      storeApiClient.deleteOrder(orderId);
      createdOrderIds.remove(orderId);
      log.info("Cleaned up order {} after test failure", orderId);
    } catch (Exception cleanupException) {
      log.warn("Failed to cleanup order {} after test failure", orderId, cleanupException);
    }
  }

  @Step("Execute order placement API call")
  protected Response executeOrderPlacement(Order orderRequest) {
    Response response = storeApiClient.placeOrder(orderRequest);
    log.debug("Order placement response status: {}", response.getStatusCode());
    return response;
  }

  @Step("Validate successful order creation and track for cleanup")
  protected void validateSuccessfulOrderCreation(Response response, Order orderRequest) {
    assertEquals(
        response.getStatusCode(), HttpURLConnection.HTTP_OK, "Order creation should succeed");

    Order responseOrder = response.as(Order.class);
    assertOrderMatch(orderRequest, responseOrder, "Order creation response");

    trackOrderForCleanup(response);
    log.info("Successfully created order with ID: {}", responseOrder.getId());
  }

  @Step("Measure order placement response time")
  protected long measureOrderPlacementTime(Order orderRequest) {
    long startTime = System.currentTimeMillis();
    Response response = storeApiClient.placeOrder(orderRequest);
    long responseTime = System.currentTimeMillis() - startTime;

    log.info(
        "Order placement completed in {}ms (threshold: {}ms)", responseTime, MAX_RESPONSE_TIME_MS);

    assertEquals(
        response.getStatusCode(),
        HttpURLConnection.HTTP_OK,
        "Order should be created successfully");

    trackOrderForCleanup(response);
    return responseTime;
  }
}
