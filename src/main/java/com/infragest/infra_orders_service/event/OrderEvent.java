package com.infragest.infra_orders_service.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.infragest.infra_orders_service.enums.OrderState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Evento publicado a RabbitMQ cuando se crea o cambia el estado de una orden.
 *
 * Se incluye {@code @JsonInclude(JsonInclude.Include.NON_NULL)} para no serializar
 * campos nulos en el mensaje.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador de la orden.
     */
    private UUID orderId;

    /**
     * Estado de la orden.
     */
    private OrderState state;

    /**
     * Descripción opcional de la orden.
     */
    private String description;

    /**
     * Tipo de assignee (nombre del enum), p. ej. "GROUP" o "EMPLOYEE".
     */
    private String assigneeType;

    /**
     * Identificador del assignee (grupo o empleado).
     */
    private UUID assigneeId;

    /**
     * Lista de deviceIds incluidos en la orden.
     */
    private List<UUID> deviceIds;

    /**
     * Lista de emails de destinatarios para notificaciones.
     */
    private List<String> recipientEmails;
}
