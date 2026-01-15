package com.infragest.infra_orders_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

/**
 * Entidad JPA que representa un equipo incluido en una orden (OrderItem).
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Entity
@Table(name = "rental_order_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OrderItem extends BaseEntity{

    /**
     * Identificador del equipo (referencia a infra-devices-service).
     */
    @Column(name = "equipment_id", nullable = false)
    @NotNull(message = "El identificador del dispositivo (deviceId) es obligatorio.")
    private UUID deviceId;

    /**
     * Estado original del equipo (por ejemplo: "GOOD", "REGULAR"). Se guarda para poder restaurar
     * el estado cuando la orden sea finalizada.
     */
    @Column(name = "original_equipment_state", length = 50)
    @NotBlank(message = "El estado original del dispositivo (originalDeviceState) no puede estar vacío.")
    private String originalDeviceState;

    /**
     * Relación hacia la orden propietaria. Fetch LAZY para evitar cargas innecesarias.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
