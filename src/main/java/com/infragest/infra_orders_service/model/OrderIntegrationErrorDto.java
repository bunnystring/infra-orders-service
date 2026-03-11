package com.infragest.infra_orders_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderIntegrationErrorDto {
    private String service;
    private String type;
    private String message;
    private Instant timestamp;
    private List<UUID> deviceIds;
    private String assignedTypeId;
    private String assignedId;

}
