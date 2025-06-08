package com.petstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Order Status Enum */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {
  @JsonProperty("placed")
  PLACED("placed"),

  @JsonProperty("approved")
  APPROVED("approved"),

  @JsonProperty("delivered")
  DELIVERED("delivered");

  private final String value;

  @Override
  public String toString() {
    return value;
  }
}
