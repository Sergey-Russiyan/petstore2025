package com.petstore.tests.constants;

/**
 * Test constants shared across all test classes Organized using nested static classes for better
 * readability and maintainability
 */
public final class TestConstants {

  // ==================== HTTP HEADERS ====================
  public static final class Headers {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String SERVER = "Server";
    public static final String DATE = "Date";

    // CORS Headers
    public static final String CORS_ORIGIN = "Access-Control-Allow-Origin";
    public static final String CORS_METHODS = "Access-Control-Allow-Methods";
    public static final String CORS_HEADERS = "Access-Control-Allow-Headers";

    private Headers() {}
  }

  // ==================== CONTENT TYPES ====================
  public static final class ContentTypes {
    public static final String JSON = "application/json";
    public static final String XML = "application/xml";
    public static final String FORM = "application/x-www-form-urlencoded";
    public static final String TEXT = "text/plain";

    private ContentTypes() {}
  }

  // ==================== PERFORMANCE THRESHOLDS ====================
  public static final class Performance {
    public static final long MAX_RESPONSE_TIME_MS = 5000L;
    public static final long FAST_RESPONSE_TIME_MS = 1000L;
    public static final long SLOW_RESPONSE_TIME_MS = 10000L;

    private Performance() {}
  }

  // ==================== TEST DATA IDS ====================
  public static final class TestIds {
    public static final long NON_EXISTENT = 999999L;
    public static final long INVALID_NEGATIVE = -1L;
    public static final long MAX_VALUE = Long.MAX_VALUE;

    private TestIds() {}
  }

  // ==================== ERROR RESPONSES ====================
  public static final class ErrorTypes {
    public static final String ERROR = "error";
    public static final String VALIDATION = "validation";
    public static final String NOT_FOUND = "not_found";

    private ErrorTypes() {}
  }

  public static final class ErrorMessages {
    public static final String ORDER_NOT_FOUND = "Order not found";
    public static final String PET_NOT_FOUND = "Pet not found";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String INVALID_INPUT = "Invalid input provided";
    public static final String UNAUTHORIZED_ACCESS = "Unauthorized access";

    private ErrorMessages() {}
  }

  // Private constructor to prevent instantiation
  private TestConstants() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }
}
