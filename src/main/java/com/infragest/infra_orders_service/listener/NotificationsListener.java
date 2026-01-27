package com.infragest.infra_orders_service.listener;

import com.infragest.infra_orders_service.config.RabbitMQConfig;
import com.infragest.infra_orders_service.event.NotificationEvent;
import com.infragest.infra_orders_service.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationsListener {

    private final OrderService orderService;

    public NotificationsListener(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Método que procesa eventos de confirmación recibidos desde la cola `orders.notifications.queue`.
     *
     * @param notificationEvent el evento de confirmación recibido desde el microservicio de notificaciones.
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE_NAME)
    public void handleNotificationEvent(NotificationEvent notificationEvent) {
        log.info("Confirmación de notificación recibida: {}", notificationEvent);

        // Procesar el evento (ejemplo: actualizar el estado de la orden en la base de datos)
        orderService.updateOrderNotificationStatus(notificationEvent);
    }
}
