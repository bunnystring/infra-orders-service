package com.infragest.infra_orders_service.excepcion;

public class GroupUnavailableExcepction extends RuntimeException {

    /**
     * Tipos de error usados por {@link GroupUnavailableExcepction}.
     */
    public enum Type {
        NOT_FOUND,
        BAD_REQUEST,
        CONFLICT, INTERNAL_SERVER
    }

    /**
     * Tipo de la excepción (clasificación).
     */
    private final GroupUnavailableExcepction.Type type;

    public GroupUnavailableExcepction(String message, GroupUnavailableExcepction.Type type) {
        super(message);
        this.type = type;
    }

    /**
     * Obtiene el tipo (clasificación) de la excepción.
     *
     * @return el {@link GroupUnavailableExcepction.Type} asociado a esta excepción
     */
    public GroupUnavailableExcepction.Type getType() {
        return type;
    }
}
