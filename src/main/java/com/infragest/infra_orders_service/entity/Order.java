package com.infragest.infra_orders_service.entity;


import com.infragest.infra_orders_service.enums.AssigneeType;
import com.infragest.infra_orders_service.enums.OrderState;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA que representa una orden de alquiler de equipos.
 * Esta clase extiende {@link BaseEntity} por lo que hereda:
 * Nota sobre claves: la PK es un UUID generado por {@link BaseEntity}. Si en tu entorno los equipos usan identificadores
 * numéricos, {@code equipmentId} se modela como UUID en {@link OrderItem}; ajusta según el contrato con infra-devices-service.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Entity
@Table(name = "rental_order", indexes = {
        @Index(name = "idx_order_state", columnList = "state"),
        @Index(name = "idx_order_assignee_id", columnList = "assignee_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, exclude = "items")
public class Order extends BaseEntity{

    /**
     * Descripción libre de la orden (máx. 1000 caracteres).
     */
    @Column(length = 1000)
    @Size(max = 1000, message = "La descripción no puede exceder de 1000 caracteres")
    private String description;

    /**
     * Estado actual de la orden.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = 50, nullable = false)
    private OrderState state;

    /**
     * Tipo de asignado (EMPLOYEE o GROUP).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_type", length = 50)
    private AssigneeType assigneeType;

    /**
     * Identificador del asignado (empleado o grupo). UUID porque la entidad hereda UUID en BaseEntity.
     * Este campo NO debe ser usado para crear empleados o grupos: infra-groups-service es el responsable
     * de la creación/gestión de esas entidades.
     */
    @Column(name = "assignee_id")
    private UUID assigneeId;

    /**
     * Items (equipos) incluidos en la orden. Se usa cascade ALL y orphanRemoval para mantener consistencia.
     * Fetch lazy por defecto para evitar cargar items innecesariamente.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Callback JPA que se ejecuta antes de persistir la entidad.
     * Inicializa el estado a CREATED si aún no está definido.
     */
    @PrePersist
    public void prePersistOrder() {
        if (state == null) {
            state = OrderState.CREATED;
        }
    }
}
