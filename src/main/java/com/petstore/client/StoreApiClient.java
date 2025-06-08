package com.petstore.client;

import static io.restassured.RestAssured.given;

import com.petstore.dto.ErrorResponse;
import com.petstore.dto.Order;
import com.petstore.util.RetryUtils;
import io.restassured.response.Response;
import java.util.Map;

/**
 * Store API Client - handles store-related endpoints Extends BaseApiClient to inherit common
 * configuration
 */
public class StoreApiClient extends BaseApiClient {

  private static final String STORE_BASE_PATH = "/store";
  private static final String INVENTORY_PATH = STORE_BASE_PATH + "/inventory";
  private static final String ORDER_PATH = STORE_BASE_PATH + "/order";

  /** GET /store/inventory Returns pet inventories by status */
  public Response getInventory() {
    return given()
        .spec(requestSpec)
        .when()
        .get(INVENTORY_PATH)
        .then()
        .spec(responseSpec)
        .extract()
        .response();
  }

  /** POST /store/order Place an order for a pet */
  public Response placeOrder(Order order) {
    return given()
        .spec(requestSpec)
        .body(order)
        .when()
        .post(ORDER_PATH)
        .then()
        .spec(responseSpec)
        .extract()
        .response();
  }

  /** GET /store/order/{orderId} Find purchase order by ID */
  public Response getOrderById(Long orderId) {
    return given()
        .spec(requestSpec)
        .pathParam("orderId", orderId)
        .when()
        .get(ORDER_PATH + "/{orderId}")
        .then()
        .spec(responseSpec)
        .extract()
        .response();
  }

  /** DELETE /store/order/{orderId} Delete purchase order by ID */
  public Response deleteOrder(Long orderId) {
    return given()
        .spec(requestSpec)
        .pathParam("orderId", orderId)
        .when()
        .delete(ORDER_PATH + "/{orderId}")
        .then()
        .spec(responseSpec)
        .extract()
        .response();
  }

  // Convenience methods for typed responses

  /** Get inventory as Map */
  public Map getInventoryAsMap() {
    return RetryUtils.retry(() -> getInventory().as(Map.class), 3, 1000); // 3 attempts, 1s delay
  }

  /** Get order by ID and return as Order object */
  public Order getOrderByIdAsObject(Long orderId) {
    Response response = getOrderById(orderId);
    if (response.getStatusCode() == 200) {
      return response.as(Order.class);
    }
    return null;
  }

  /** Get error response when order is not found */
  public ErrorResponse getOrderByIdAsError(Long orderId) {
    Response response = getOrderById(orderId);
    if (response.getStatusCode() != 200) {
      return response.as(ErrorResponse.class);
    }
    return null;
  }

  /** Place order and return as Order object */
  public Order placeOrderAsObject(Order order) {
    return RetryUtils.retry(
        () -> {
          Response response = placeOrder(order);
          if (response.getStatusCode() == 200) {
            return response.as(Order.class);
          }
          throw new RuntimeException("Failed to place order: " + response.getStatusLine());
        },
        3,
        1000);
  }
}
