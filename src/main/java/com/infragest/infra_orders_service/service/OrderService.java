package com.infragest.infra_orders_service.service;

import com.infragest.infra_orders_service.enums.OrderState;
import com.infragest.infra_orders_service.event.NotificationEvent;
import com.infragest.infra_orders_service.model.OrderRq;
import com.infragest.infra_orders_service.model.OrderRs;

import java.util.List;
import java.util.UUID;

/**
 * Interfaz de servicio para la lógica de negocio relacionada con las órdenes.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
public interface OrderService {

    /**
     * Crea una nueva orden a partir del request DTO.
     *
     * @param rq petición de creación de la orden
     * @return representación de la orden persistida (OrderRs)
     * @throws RuntimeException en caso de fallo de validación o error de negocio (usar excepciones parametrizadas)
     */
    OrderRs createOrder(OrderRq rq);

    /**
     * Lista todas las órdenes.
     *
     * @return lista de {@link OrderRs}
     */
    List<OrderRs> listOrders();

    /**
     * Obtiene una orden por su identificador UUID.
     *
     * @param id UUID de la orden
     * @return Optional con {@link OrderRs} si existe, vacío si no existe
     */
    OrderRs getOrder(UUID id);

    /**
     * Cambia el estado de una orden y ejecuta las acciones asociadas al cambio.
     *
     * Comportamiento esperado:
     * - Si el nuevo estado es FINISHED, restaurar los estados originales de los equipos en infra-devices-service.
     * - Publicar evento de cambio de estado en RabbitMQ.
     *
     * @param orderId UUID de la orden
     * @param newState nuevo estado a aplicar
     * @return la representación actualizada {@link OrderRs}
     * @throws RuntimeException si la orden no existe o si la transición no es válida
     */
    OrderRs changeState(UUID orderId, OrderState newState);

    /**
     * Obtiene las órdenes asociadas a un assignee (empleado o grupo).
     *
     * @param assigneeId UUID del assignee
     * @return lista de {@link OrderRs}
     */
    List<OrderRs> findByAssigneeId(UUID assigneeId);

    /**
     * Obtiene las órdenes que incluyen un equipmentId dado.
     *
     * @param equipmentId UUID del equipo
     * @return lista de {@link OrderRs}
     */
    List<OrderRs> findByEquipmentId(UUID equipmentId);

    /**
     * Actualiza el estado de la notificación en las órdenes.
     *
     * @param notificationEvent evento que contiene los detalles de la confirmación de notificación.
     */
    void updateOrderNotificationStatus(NotificationEvent notificationEvent);
}
