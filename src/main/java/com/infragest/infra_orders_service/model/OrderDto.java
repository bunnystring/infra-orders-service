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
 * DTO genérico que representa la estructura de una orden y que puede
 * reutilizarse tanto en peticiones como en respuestas cuando aplique.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {

    /**
     * UUID de la orden. En creaciones (request) puede estar ausente.
     */
    private UUID id;

    /**
     * Descripción opcional de la orden.
     */
    private String description;

    /**
     * Estado de la orden. En requests normalmente se omite y se inicializa a CREATED.
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
     * Timestamps heredados de BaseEntity.
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Lista de items incluidos en la orden.
     */
    private List<OrderItemDto> items;

}
