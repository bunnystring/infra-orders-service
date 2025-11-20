package com.infragest.infra_orders_service.repository;

import com.infragest.infra_orders_service.entity.Order;
import com.infragest.infra_orders_service.enums.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data para la entidad {@link Order}.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Devuelve las órdenes asociadas a un assignee (empleado o grupo).
     *
     * @param assigneeId UUID del empleado o del grupo
     * @return lista de órdenes (puede estar vacía)
     */
    List<Order> findByAssigneeId(UUID assigneeId);

    /**
     * Devuelve las órdenes con el estado especificado.
     *
     * @param state estado de la orden
     * @return lista de órdenes con ese estado
     */
    List<Order> findByState(OrderState state);

    /**
     * Devuelve las órdenes que contienen un deviceId específico en sus items.
     * Usa JPQL con join sobre la colección items.
     *
     * @param deviceId UUID del equipo
     * @return lista de órdenes que incluyen ese equipo
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.items i WHERE i.deviceId = :deviceId")
    List<Order> findByDeviceId(@Param("deviceId") UUID deviceId);
}
