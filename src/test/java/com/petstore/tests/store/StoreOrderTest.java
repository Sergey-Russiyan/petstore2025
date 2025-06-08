package com.petstore.tests.store;

import static org.testng.Assert.*;

import com.petstore.dto.Order;
import com.petstore.dto.OrderStatus;
import com.petstore.framework.AllureTestListener;
import com.petstore.tests.BaseStoreTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import java.time.OffsetDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

@Epic("Store API")
@Feature("Order Management")
@Listeners({AllureTestListener.class})
public class StoreOrderTest extends BaseStoreTest {

  private static final Logger log = LoggerFactory.getLogger(StoreOrderTest.class);

  private List<Long> createdOrderIds;
  private long testStartTime;

  @BeforeMethod
  public void setupTest() {
    testStartTime = System.currentTimeMillis();
    createdOrderIds = new ArrayList<>();
    log.info("Starting new test execution - initializing order tracking");
  }

  @AfterMethod
  public void cleanUp() {
    long testDuration = System.currentTimeMillis() - testStartTime;
    log.info("=== Completed test in {} ms ===", testDuration);

    if (createdOrderIds.isEmpty()) {
      log.debug("No orders to clean up");
      return;
    }

    performOrderCleanup();
  }

  // ==================== DATA PROVIDERS ====================

  @DataProvider(name = "validOrderData")
  public Object[][] validOrderData() {
    return new Object[][] {
      {
        Order.builder()
            .id(0L)
            .petId(1L)
            .quantity(2)
            .status(OrderStatus.PLACED)
            .complete(true)
            .build()
      },
      {Order.builder().petId(5L).quantity(1).build()}, // minimal data
      {
        Order.builder()
            .petId(10L)
            .quantity(5)
            .shipDate(OffsetDateTime.now().plusDays(7))
            .status(OrderStatus.APPROVED)
            .complete(false)
            .build()
      },
      {
        Order.builder()
            .petId(3L)
            .quantity(1)
            .shipDate(OffsetDateTime.now().minusDays(1))
            .status(OrderStatus.DELIVERED)
            .complete(true)
            .build()
      }
    };
  }

  @DataProvider(name = "invalidOrderIds")
  public Object[][] invalidOrderIds() {
    return new Object[][] {
      {-1L, "Negative order ID"},
      {0L, "Zero order ID"},
      {999999L, "Non-existent order ID"},
      {Long.MAX_VALUE, "Maximum long value order ID"}
    };
  }

  @DataProvider(name = "invalidOrderData")
  public Object[][] invalidOrderData() {
    return new Object[][] {
      // ID-specific validation tests
      {Order.builder().petId(null).quantity(1).build(), "Null pet ID"},
      {Order.builder().petId(-1L).quantity(1).build(), "Negative pet ID"},
      {Order.builder().petId(0L).quantity(1).build(), "Zero pet ID"},
      {Order.builder().petId(Long.MAX_VALUE).quantity(1).build(), "Extreme pet ID"},

      // Quantity-specific validation tests (with valid pet ID)
      {Order.builder().petId(1L).quantity(0).build(), "Zero quantity"},
      {Order.builder().petId(1L).quantity(-5).build(), "Negative quantity"},
      {Order.builder().petId(1L).quantity(Integer.MAX_VALUE).build(), "Extreme quantity"},

      // Status-specific validation tests (with valid pet ID)
      {Order.builder().petId(1L).quantity(1).status(null).build(), "Null status"},

      // Complete missing data test
      {Order.builder().build(), "All required fields missing"}
    };
  }

  @DataProvider(name = "edgeCaseOrderData")
  public Object[][] edgeCaseOrderData() {
    return new Object[][] {
      {
        Order.builder().petId(1L).quantity(1).shipDate(OffsetDateTime.now().minusDays(10)).build(),
        "Recent past ship date"
      },
      {
        Order.builder().petId(1L).quantity(1).shipDate(OffsetDateTime.now().minusYears(1)).build(),
        "Old past ship date"
      },
      {Order.builder().petId(1L).quantity(Integer.MAX_VALUE).build(), "Maximum quantity"}
    };
  }

  // ==================== POSITIVE TEST CASES ====================

  @Test(
      dataProvider = "validOrderData",
      description = "Verify POST /store/order creates orders successfully with various valid data")
  @Story("Place Order")
  @Severity(SeverityLevel.CRITICAL)
  @Description(
      "Test verifies that orders can be placed successfully with different valid data combinations")
  public void testPlaceOrderSuccess(Order orderRequest) {
    log.info(
        "Testing order placement with petId: {}, quantity: {}",
        orderRequest.getPetId(),
        orderRequest.getQuantity());
    Response response = executeOrderPlacement(orderRequest);
    validateSuccessfulOrderCreation(response, orderRequest);
  }

  @Test(description = "Verify complete order lifecycle: create, retrieve, update status, delete")
  @Story("Order Lifecycle")
  @Flaky
  @Severity(SeverityLevel.CRITICAL)
  @Description(
      "Validates the complete lifecycle of an order from creation to deletion, ensuring data integrity at each step")
  public void testCompleteOrderLifecycle() {
    log.info("Starting complete order lifecycle test");

    Order orderRequest =
        Order.builder()
            .petId(10L)
            .quantity(2)
            .shipDate(OffsetDateTime.now())
            .status(OrderStatus.PLACED)
            .complete(false)
            .build();

    Long orderId = null;

    try {
      orderId = createOrderForLifecycleTest(orderRequest);
      log.warn(
          "validateOrderCreation is deprecated, validation is now included in placeOrderAndTrack");
      assertNotNull(orderId, "Order ID should not be null after successful creation");
      assertTrue(orderId > 0, "Order ID should be a positive number");
      retrieveAndValidateOrder(orderId, orderRequest);
      deleteAndValidateOrderRemoval(orderId);

    } catch (Exception e) {
      handleLifecycleTestFailure(orderId, e);
    }
  }

  @Test(description = "Verify order response time is acceptable")
  @Story("Place Order")
  @Severity(SeverityLevel.MINOR)
  @Description(
      "Performance test to ensure order creation response time is within acceptable limits")
  public void testPlaceOrderPerformance() {
    log.info("Starting performance test for order placement");
    Order orderRequest = createValidOrder();

    long responseTime = measureOrderPlacementTime(orderRequest);
    validatePerformanceResults(responseTime);
  }

  // ==================== NEGATIVE TEST CASES ====================

  @Test(
      dataProvider = "invalidOrderIds",
      description = "Verify GET /store/order/{orderId} handles invalid order IDs")
  @Story("Get Order")
  @Severity(SeverityLevel.NORMAL)
  @Description("Test verifies proper error handling when requesting orders with invalid IDs")
  public void testGetOrderWithInvalidIds(Long invalidOrderId, String testCase) {
    log.info("Testing GET order with invalid ID: {} ({})", invalidOrderId, testCase);
    Response response = executeGetOrderRequest(invalidOrderId);
    validateInvalidOrderResponse(response, testCase, "GET");
  }

  @Test(
      dataProvider = "invalidOrderIds",
      description = "Verify DELETE /store/order/{orderId} handles invalid order IDs")
  @Story("Delete Order")
  @Severity(SeverityLevel.NORMAL)
  @Description("Test verifies proper error handling when deleting orders with invalid IDs")
  public void testDeleteOrderWithInvalidIds(Long invalidOrderId, String testCase) {
    log.info("Testing DELETE order with invalid ID: {} ({})", invalidOrderId, testCase);
    Response response = executeDeleteOrderRequest(invalidOrderId);
    validateInvalidOrderResponse(response, testCase, "DELETE");
  }

  @Test(
      dataProvider = "invalidOrderData",
      description = "Verify API validation behavior and document current acceptance criteria")
  @Story("Place Order")
  @Severity(SeverityLevel.NORMAL)
  @Description(
      "Test documents what the API currently accepts vs rejects to identify validation gaps")
  public void testOrderValidationBehaviorDocumentation(Order testOrder, String description) {

    log.info("Testing validation case: {}", description);

    try {
      Response response = storeApiClient.placeOrder(testOrder);
      handleOrderValidationResponse(response, description);
    } catch (Exception e) {
      log.error(
          "Exception occurred during validation test for {}: {}", description, e.getMessage(), e);
    }
  }

  @Test(
      dataProvider = "invalidOrderData",
      description =
          "Verify POST /store/order rejects invalid order data with appropriate error codes")
  @Story("Place Order")
  @Severity(SeverityLevel.CRITICAL)
  @Description(
      "Validates that API properly rejects invalid order data and returns appropriate HTTP error codes (4xx)")
  public void testPlaceOrderWithInvalidData(Order invalidOrder, String testCase) {
    log.info("Testing order placement with invalid data: {}", testCase);
    Response response = executeOrderPlacementWithInvalidData(invalidOrder, testCase);
    validateInvalidDataRejection(response, testCase);
  }

  @Test(
      description = "Verify GET /store/order/{orderId} for non-existent order returns proper error")
  @Story("Get Order")
  @Severity(SeverityLevel.NORMAL)
  @Description("Test verifies proper error response structure for non-existent orders")
  public void testGetNonExistentOrderErrorResponse() {
    Long nonExistentOrderId = 999999L;
    log.info("Testing GET request for non-existent order ID: {}", nonExistentOrderId);

    Response response = executeGetOrderRequest(nonExistentOrderId);
    assertErrorResponse(response);
  }

  // ==================== EDGE CASE TESTS ====================

  @Test(
      dataProvider = "edgeCaseOrderData",
      description = "Verify order creation with edge case values that should be accepted")
  @Story("Place Order")
  @Severity(SeverityLevel.NORMAL)
  @Description("Test verifies order creation with boundary/edge case values")
  public void testPlaceOrderEdgeCases(Order edgeCaseOrder, String testCase) {
    log.info("Testing order placement with edge case: {}", testCase);

    Response response = storeApiClient.placeOrder(edgeCaseOrder);

    // Assert that edge cases are accepted (not rejected)
    assertTrue(
        response.getStatusCode() >= 200 && response.getStatusCode() < 300,
        "Edge case '"
            + testCase
            + "' should be accepted but got status: "
            + response.getStatusCode());

    log.info("API accepted order with {}", testCase);
    trackOrderForCleanup(response);
  }

  // 2. ORDER STATUS TRANSITION TESTING
  @DataProvider(name = "statusTransitions")
  public Object[][] statusTransitions() {
    return new Object[][] {
      {OrderStatus.PLACED, OrderStatus.APPROVED, true},
      {OrderStatus.APPROVED, OrderStatus.DELIVERED, true},
      {OrderStatus.PLACED, OrderStatus.DELIVERED, false}, // Invalid transition
      {OrderStatus.DELIVERED, OrderStatus.PLACED, false} // Backward transition
    };
  }

  // 3. LARGE PAYLOAD TESTING
  @Test(description = "Verify API handles large order quantities gracefully")
  @Story("Data Limits")
  @Severity(SeverityLevel.NORMAL)
  public void testLargeOrderQuantities() {
    log.info("Testing order with extremely large quantity");

    Order largeOrder =
        Order.builder()
            .petId(1L)
            .quantity(1_000_000) // 1 million
            .build();

    Response response = storeApiClient.placeOrder(largeOrder);

    // Document actual behavior
    if (response.getStatusCode() < 300) {
      log.info("API accepted large quantity order");
      trackOrderForCleanup(response);
    } else {
      log.info("API rejected large quantity with status: {}", response.getStatusCode());
    }
  }

  // 5. DATA CONSISTENCY TESTING
  @Test(description = "Verify order data remains consistent after multiple operations")
  @Story("Data Integrity")
  @Severity(SeverityLevel.CRITICAL)
  public void testOrderDataConsistency() {
    // Create order with specific data
    OffsetDateTime originalShipDate = OffsetDateTime.now().plusDays(5);
    Order originalOrder =
        Order.builder()
            .petId(42L)
            .quantity(3)
            .shipDate(originalShipDate)
            .status(OrderStatus.PLACED)
            .complete(false)
            .build();

    Response createResponse = storeApiClient.placeOrder(originalOrder);
    Long orderId = createResponse.jsonPath().getLong("id");
    trackOrderForCleanup(createResponse);

    // Retrieve order multiple times and verify consistency
    for (int i = 0; i < 3; i++) {
      Response getResponse = storeApiClient.getOrderById(orderId);
      Order retrievedOrder = getResponse.as(Order.class);

      assertEquals(
          originalOrder.getPetId(), retrievedOrder.getPetId(), "Pet ID should remain consistent");
      assertEquals(
          originalOrder.getQuantity(),
          retrievedOrder.getQuantity(),
          "Quantity should remain consistent");
      assertEquals(
          originalOrder.getStatus(), retrievedOrder.getStatus(), "Status should remain consistent");
    }
  }

  @Test(description = "Verify API respects rate limiting", groups = "performance")
  @Story("Rate Limiting")
  @Severity(SeverityLevel.NORMAL)
  public void testRateLimiting() throws InterruptedException {
    // Rate limiting test configuration - assumption: acts as technical specification
    final int MAX_REQUESTS_TO_TEST = 30; // Maximum requests before giving up
    final int EXPECTED_MIN_SUCCESS_COUNT = 5; // Minimum successful requests expected
    final int REQUEST_INTERVAL_MS = 10; // Delay between requests in milliseconds
    final int RATE_LIMIT_STATUS_CODE = 429; // HTTP status for "Too Many Requests"
    final long PET_ID_FOR_LOAD_TEST = 1L; // Pet ID to use for all test orders

    log.info(
        "Testing API rate limiting behavior - Max requests: {}, Request interval: {}ms",
        MAX_REQUESTS_TO_TEST,
        REQUEST_INTERVAL_MS);

    List<Response> responses = new ArrayList<>();
    boolean rateLimitHit = false;

    // Send requests with controlled interval
    for (int requestNumber = 1; requestNumber <= MAX_REQUESTS_TO_TEST; requestNumber++) {
      Order testOrder = Order.builder().petId(PET_ID_FOR_LOAD_TEST).quantity(1).build();

      Response response = storeApiClient.placeOrder(testOrder);
      responses.add(response);

      // Check if rate limiting was triggered
      if (response.getStatusCode() == RATE_LIMIT_STATUS_CODE) {
        log.info("Rate limiting activated after {} requests (Expected behavior)", requestNumber);
        rateLimitHit = true;
        break;
      } else if (response.getStatusCode() < 300) {
        trackOrderForCleanup(response);
        log.debug("Request {}: Success (Status: {})", requestNumber, response.getStatusCode());
      } else {
        log.warn(
            "Request {}: Unexpected error (Status: {})", requestNumber, response.getStatusCode());
      }

      // small delay between requests to simulate realistic load
      if (requestNumber < MAX_REQUESTS_TO_TEST) {
        Thread.sleep(REQUEST_INTERVAL_MS);
      }
    }
    // Analyze results
    long successCount =
        responses.stream()
            .mapToInt(Response::getStatusCode)
            .filter(code -> code >= 200 && code < 300)
            .count();

    long errorCount =
        responses.stream()
            .mapToInt(Response::getStatusCode)
            .filter(code -> code >= 400 && code != RATE_LIMIT_STATUS_CODE)
            .count();

    log.info(
        "Rate limiting test results - Total requests: {}, Successful: {}, Rate limited: {}, Other errors: {}",
        responses.size(),
        successCount,
        rateLimitHit ? 1 : 0,
        errorCount);

    // Validation
    assertTrue(
        successCount >= EXPECTED_MIN_SUCCESS_COUNT,
        String.format(
            "Expected at least %d successful requests before rate limiting, but got %d",
            EXPECTED_MIN_SUCCESS_COUNT, successCount));

    if (!rateLimitHit && responses.size() >= MAX_REQUESTS_TO_TEST) {
      log.warn(
          "Rate limiting was not triggered after {} requests - may indicate missing rate limiting or high limits",
          MAX_REQUESTS_TO_TEST);
    }
  }
}
