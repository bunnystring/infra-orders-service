package com.infragest.infra_orders_service.util;

/**
 * Mensajes de error reutilizables para el módulo de órdenes.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
public abstract class MessageException {

    private MessageException() {}

    // Ordenes
    public static final String ORDER_NOT_FOUND = "Orden no encontrada: %s";
    public static final String ORDER_CREATE_FAILED = "No se pudo crear la orden: %s";
    public static final String ORDER_STATE_TRANSITION_INVALID = "Transición de estado inválida para la orden %s: %s";
    public static final String ORDER_ALREADY_FINALIZED = "La orden %s ya está finalizada";

    // Equipos / Devices
    public static final String EQUIPMENT_NOT_FOUND = "Equipo no encontrado: %s";
    public static final String EQUIPMENT_NOT_AVAILABLE = "Los siguientes equipos no están disponibles: %s";
    public static final String EQUIPMENT_RESERVE_FAILED = "No se pudieron reservar los equipos: %s";
    public static final String EQUIPMENT_RESTORE_FAILED = "No se pudieron restaurar los estados de los equipos: %s";
    public static final String INVALID_EQUIPMENT_LIST = "Lista de equipos inválida";

    // Assignee (employee / group)
    public static final String ASSIGNEE_REQUIRED = "El assignee (tipo e id) es requerido";
    public static final String ASSIGNEE_NOT_FOUND = "Assignee no encontrado: %s";
    public static final String ASSIGNEE_INVALID = "Assignee inválido o en estado no permitido: %s";
    public static final String GROUP_NO_MEMBERS = "El grupo %s no tiene miembros con email";
    public static final String EMPLOYEE_NOT_ACTIVE = "El empleado %s no está activo";
    public static final String EMPLOYEE_NO_EMAIL = "El empleado %s no tiene email registrado";
    public static final String GROUP_NOT_FOUND = "El grupo %s no fue encontrado.";
    public static final String EMPLOYEE_INACTIVE = "El empleado %s no está activo.";
    public static final String EMPLOYEE_NOT_FOUND = "El empleado %s no fue encontrado.";

    // Request / validation / UUID
    public static final String INVALID_REQUEST = "Request inválido";
    public static final String INVALID_UUID = "Identificador inválido: %s";
    public static final String MISSING_PARAMETER = "Falta el parámetro requerido: %s";

    // Operación / permisos / DB / interno
    public static final String OPERATION_NOT_ALLOWED = "Operación no permitida: %s";
    public static final String DATABASE_ERROR = "Error en la base de datos";
    public static final String INTERNAL_ERROR = "Error interno del servidor";
    public static final String DEPENDENCY_ERROR = "Error al comunicarse con un servicio dependiente.";

}
