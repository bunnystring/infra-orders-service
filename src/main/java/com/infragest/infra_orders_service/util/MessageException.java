package com.infragest.infra_orders_service.util;

/**
 * Mensajes de error reutilizables para el módulo de órdenes.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
public abstract class MessageException {

    private MessageException() {}

    // Orders
    public static final String SERVICE_UNAVAILABLE = "The service is currently unavailable.";
    public static final String ORDER_NOT_FOUND = "Order not found: %s";
    public static final String ORDER_CREATE_FAILED = "Failed to create the order: %s";
    public static final String ORDER_STATE_TRANSITION_INVALID = "Invalid state transition for order %s: %s";
    public static final String ORDER_ALREADY_FINALIZED = "Order %s is already finalized";

    // Devices / Equipment
    public static final String DEVICE_ERROR_COMMUNICATION = "Error communicating with the devices service.";
    public static final String EQUIPMENT_NOT_FOUND = "Equipment not found: %s";
    public static final String EQUIPMENT_NOT_AVAILABLE = "The following equipment is not available: %s";
    public static final String EQUIPMENT_RESERVE_FAILED = "Failed to reserve the equipment: %s";
    public static final String EQUIPMENT_RESTORE_FAILED = "Failed to restore the state of the equipment: %s";
    public static final String INVALID_EQUIPMENT_LIST = "Invalid equipment list";
    public static final String DEVICE_NOT_FOUND_BY_IDS = "";

    // Assignee (employee / group)
    public static final String ASSIGNEE_REQUIRED = "The assignee (type and ID) is required";
    public static final String ASSIGNEE_NOT_FOUND = "Assignee not found: %s";
    public static final String ASSIGNEE_INVALID = "Invalid or disallowed assignee status: %s";
    public static final String GROUP_NO_MEMBERS = "Group %s has no members with valid emails";
    public static final String EMPLOYEE_NOT_ACTIVE = "Employee %s is not active";
    public static final String EMPLOYEE_NO_EMAIL = "Employee %s has no registered email";
    public static final String GROUP_NOT_FOUND = "Group %s was not found.";
    public static final String EMPLOYEE_INACTIVE = "Employee %s is not active.";
    public static final String EMPLOYEE_NOT_FOUND = "Employee %s was not found.";

    // Request / validation / UUID
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String INVALID_UUID = "Invalid identifier: %s";
    public static final String MISSING_PARAMETER = "Missing required parameter: %s";

    // Operation / permissions / DB / internal
    public static final String OPERATION_NOT_ALLOWED = "Operation not allowed: %s";
    public static final String DATABASE_ERROR = "Database error";
    public static final String INTERNAL_ERROR = "Internal server error";
    public static final String DEPENDENCY_ERROR = "Error communicating with a dependent service.";
}
