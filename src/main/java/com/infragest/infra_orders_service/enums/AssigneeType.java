package com.infragest.infra_orders_service.enums;

/**
 * Tipo de entidad a la que puede asignarse una orden.
 * Orders Service debe validar la existencia/estado del assignee consultando infra-groups-service;
 * no debe crear ni modificar empleados/grupos.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
public enum AssigneeType {
    EMPLOYEE,
    GROUP
}
