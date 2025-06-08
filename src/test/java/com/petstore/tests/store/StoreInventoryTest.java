package com.petstore.tests.store;

import static com.petstore.tests.constants.TestConstants.Performance.MAX_RESPONSE_TIME_MS;

import com.petstore.dto.Order;
import com.petstore.framework.AllureTestListener;
import com.petstore.tests.BaseStoreTest;
import io.qameta.allure.*;
import io.restassured.response.Response;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

@Epic("Store API")
@Feature("Inventory Management")
@Listeners({AllureTestListener.class})
public class StoreInventoryTest extends BaseStoreTest {

  private static final Logger log = LoggerFactory.getLogger(StoreInventoryTest.class);
  private long testStartTime;

  @BeforeMethod
  public void setupTest() {
    testStartTime = System.currentTimeMillis();
    log.info("Starting new test execution - inventory tests");
  }

  @AfterMethod
  public void teardownTest() {
    long testDuration = System.currentTimeMillis() - testStartTime;
    log.info("=== Completed test in {} ms ===", testDuration);
  }

  @Test(description = "Verify GET /store/inventory returns valid response")
  @Story("Get Inventory")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Test verifies inventory endpoint returns valid response with expected structure")
  public void testGetInventorySuccess() {
    log.info("Testing inventory endpoint for successful response");
    Response response = executeInventoryRequest();
    validateInventoryResponse(response);
  }

  @Test(description = "Verify inventory after placing a new order")
  @Story("Get Inventory")
  @Severity(SeverityLevel.CRITICAL)
  @Description("Test verifies inventory changes after placing a new order")
  public void testInventoryAfterOrderPlacement() {
    log.info("Testing inventory changes after order placement");
    Map<String, Integer> initialInventory = retrieveInventory();
    Order testOrder = createTestOrder();
    Response orderResponse = placeTestOrder(testOrder);
    validateInventoryChanges(initialInventory, orderResponse);
  }

  @Test(description = "Verify inventory response time is acceptable")
  @Story("Get Inventory")
  @Severity(SeverityLevel.NORMAL)
  @Description("Performance test to ensure inventory response time is within acceptable limits")
  public void testGetInventoryResponseTime() {
    log.info("Testing inventory endpoint response time (max: {}ms)", MAX_RESPONSE_TIME_MS);
    long responseTime = measureInventoryResponseTime();
    validatePerformanceResults(responseTime);
  }

  @Test(description = "Verify inventory contains expected pet statuses")
  @Story("Get Inventory")
  @Severity(SeverityLevel.NORMAL)
  @Description("Test verifies inventory contains all expected pet statuses")
  public void testInventoryContainsExpectedStatuses() {
    Map<String, Integer> inventory = retrieveInventory();
    validateExpectedStatuses(inventory);
  }

  @Test(description = "Verify inventory data consistency between multiple calls")
  @Story("Get Inventory")
  @Severity(SeverityLevel.NORMAL)
  @Description("Test verifies inventory data remains consistent across multiple requests")
  public void testInventoryDataConsistency() {
    log.info("Testing inventory data consistency across multiple calls");
    Map<String, Integer> inventory1 = retrieveInventory();
    int delayBetweenCalls = 100;
    addTestDelay(delayBetweenCalls);
    Map<String, Integer> inventory2 = retrieveInventory();
    validateConsistency(inventory1, inventory2);
  }

  @Test(description = "Verify inventory response headers")
  @Story("Get Inventory")
  @Severity(SeverityLevel.MINOR)
  @Description("Test verifies all required headers are present in inventory response")
  public void testInventoryResponseHeaders() {
    log.info("Testing inventory response headers compliance");
    Response response = executeInventoryRequest();
    validateResponseHeaders(response);
  }
}
