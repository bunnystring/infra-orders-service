package com.infragest.infra_orders_service.model;

import com.infragest.infra_orders_service.enums.AssigneeType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO para peticiones de creación/actualización de orden (Request).
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRq {

    /**
     * Descripción opcional para la orden.
     */
    @Size(max = 1000, message = "La descripción debe tener máximo 1000 caracteres.")
    private String description;

    /**
     * Lista de equipos a incluir en la orden. Debe contener al menos un UUID.
     */
    @NotEmpty(message = "Debe especificar al menos un equipo para la orden.")
    private List<UUID> devicesIds;

    /**
     * Tipo del assignee (EMPLOYEE | GROUP).
     */
    @NotNull(message = "El tipo de assignee es requerido.")
    private AssigneeType assigneeType;

    /**
     * UUID del empleado o del grupo al que se asigna la orden.
     */
    @NotNull(message = "El id del assignee es requerido.")
    private UUID assigneeId;

}
