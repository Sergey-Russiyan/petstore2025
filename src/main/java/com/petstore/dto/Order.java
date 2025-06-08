package com.petstore.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Data Transfer Object for Order */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("petId")
  private Long petId;

  @JsonProperty("quantity")
  private Integer quantity;

  @JsonProperty("shipDate")
  private OffsetDateTime shipDate;

  @JsonProperty("status")
  private OrderStatus status;

  @JsonProperty("complete")
  private Boolean complete;
}
