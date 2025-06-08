package com.petstore.util;

import java.util.function.Supplier;

public class RetryUtils {

  public static <T> T retry(Supplier<T> action, int maxAttempts, long delayMillis) {
    RuntimeException lastException = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        return action.get();
      } catch (RuntimeException ex) {
        lastException = ex;
        if (attempt < maxAttempts) {
          try {
            Thread.sleep(delayMillis);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", ie);
          }
        }
      }
    }

    assert lastException != null;
    throw lastException; // All attempts failed
  }
}
