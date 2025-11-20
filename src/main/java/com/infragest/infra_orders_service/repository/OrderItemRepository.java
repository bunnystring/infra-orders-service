package com.infragest.infra_orders_service.repository;

import com.infragest.infra_orders_service.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data para la entidad {@link OrderItem}.
 *
 * Provee operaciones CRUD y consultas auxiliares para recuperar items por orden o por equipment.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Obtiene los items que pertenecen a una orden.
     *
     * @param orderId UUID de la orden
     * @return lista de items (puede ser vacía)
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Obtiene los items cuyo deviceId coincide.
     *
     * @param deviceId UUID del equipo
     * @return lista de items que referencian ese equipo
     */
    List<OrderItem> findByDeviceId(UUID deviceId);

}
