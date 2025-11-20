package com.infragest.infra_orders_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO que representa un item de orden en las respuestas.
 *
 * <p>Contiene el identificador del equipo y el estado original (antes de reservar).</p>
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
    /**
     * UUID del equipo (referencia a infra-devices-service).
     */
    private UUID deviceId;

    /**
     * Estado original del equipo antes de reservarlo (por ejemplo: "GOOD", "REGULAR").
     */
    private String originalDeviceState;
}
