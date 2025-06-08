package com.petstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

/** Data Transfer Object for API Error Response */
@Setter
@Getter
public class ErrorResponse {
  // Getters and Setters
  @JsonProperty("code")
  private Integer code;

  @JsonProperty("type")
  private String type;

  @JsonProperty("message")
  private String message;

  // Default constructor
  public ErrorResponse() {}

  // Constructor with all fields
  public ErrorResponse(Integer code, String type, String message) {
    this.code = code;
    this.type = type;
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ErrorResponse that = (ErrorResponse) o;
    return Objects.equals(code, that.code)
        && Objects.equals(type, that.type)
        && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, type, message);
  }

  @Override
  public String toString() {
    return "ErrorResponse{"
        + "code="
        + code
        + ", type='"
        + type
        + '\''
        + ", message='"
        + message
        + '\''
        + '}';
  }
}
