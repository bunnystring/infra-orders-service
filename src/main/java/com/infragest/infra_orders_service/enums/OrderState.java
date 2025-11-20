package com.infragest.infra_orders_service.enums;

/**
 * Enumeración de los posibles estados de una orden de alquiler.
 * Estos valores se usan tanto en persistencia como en los eventos publicados a RabbitMQ.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
public enum OrderState {
    CREATED,
    IN_PROCESS,
    DISPATCHED,
    FINISHED
}
