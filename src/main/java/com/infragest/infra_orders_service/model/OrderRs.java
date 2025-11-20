package com.infragest.infra_orders_service.model;

import com.infragest.infra_orders_service.enums.AssigneeType;
import com.infragest.infra_orders_service.enums.OrderState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de respuesta para representar una orden con sus items y metadatos.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRs {

    /**
     * UUID de la orden.
     */
    private UUID id;

    /**
     * Descripción libre de la orden.
     */
    private String description;

    /**
     * Estado actual de la orden (CREATED, IN_PROCESS, ...).
     */
    private OrderState state;

    /**
     * Tipo del assignee (EMPLOYEE | GROUP).
     */
    private AssigneeType assigneeType;

    /**
     * UUID del empleado o del grupo asignado.
     */
    private UUID assigneeId;

    /**
     * Fecha/hora de creación (heredada de BaseEntity).
     */
    private LocalDateTime createdAt;

    /**
     * Fecha/hora última actualización (heredada de BaseEntity).
     */
    private LocalDateTime updatedAt;

    /**
     * Equipos incluidos en la orden con su estado original.
     */
    private List<OrderItemDto> items;

}
