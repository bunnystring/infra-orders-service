package com.infragest.infra_orders_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para representar un Device en las respuestas de la API.
 * Contiene los campos que se devolverán al cliente tras operaciones como
 * crear o consultar un dispositivo.
 *
 * @author bunnystring
 * @since 2025-11-06
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRs {

    /**
     * Identificador único del dispositivo.
     */
    private UUID id;

    /**
     * Nombre descriptivo del dispositivo.
     */
    private String name;

    /**
     * Marca del dispositivo.
     */
    private String brand;

    /**
     * Código de barras único del dispositivo.
     */
    private String barcode;

    /**
     * Estado actual del dispositivo.
     */
    private DeviceStatusEnum status;

    /**
     * Fecha/hora de creación (UTC).
     */
    private LocalDateTime createdAt;

    /**
     * Fecha/hora de última actualización (UTC).
     */
    private LocalDateTime updatedAt;
}
