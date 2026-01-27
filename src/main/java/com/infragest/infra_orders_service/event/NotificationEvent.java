package com.infragest.infra_orders_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    /**
     * ID del pedido relacionado con la notificación.
     */
    private UUID orderId;

    /**
     * Estado de la notificación (ejemplo: "SUCCESS", "FAILED").
     */
    private String status;

    /**
     * Mensaje adicional para el log o auditoría.
     */
    private String message;

}
