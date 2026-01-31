package com.infragest.infra_orders_service.excepcion;

/**
 * Excepción de negocio para el módulo Order.
 * unchecked para permitir rollback automático en transacciones gestionadas por Spring.
 * Contiene un {@link Type} que clasifica el error.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
public class OrderException extends RuntimeException {

    /**
     * Tipos de error usados por {@link OrderException}.
     */
    public enum Type {
        NOT_FOUND,
        BAD_REQUEST,
        CONFLICT,
        INTERNAL_SERVER
    }

    /**
     * Tipo de la excepción (clasificación).
     */
    private final Type type;

    /**
     * Crea una nueva {@code OrderException}.
     *
     * @param message mensaje descriptivo del error
     * @param type    tipo de excepción
     */
    public OrderException(String message, Type type) {
        super(message);
        this.type = type;
    }

    /**
     * Obtiene el tipo (clasificación) de la excepción.
     *
     * @return el {@link Type} asociado a esta excepción
     */
    public Type getType() {
        return type;
    }
}
