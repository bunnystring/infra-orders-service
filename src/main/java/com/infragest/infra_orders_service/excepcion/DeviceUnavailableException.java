package com.infragest.infra_orders_service.excepcion;

public class DeviceUnavailableException extends RuntimeException {

    /**
    * Tipos de error usados por {@link DeviceUnavailableException}.
    */
    public enum Type {
    NOT_FOUND,
    BAD_REQUEST,
    CONFLICT, INTERNAL_SERVER
    }

    /**
    * Tipo de la excepción (clasificación).
    */
    private final DeviceUnavailableException.Type type;

    public DeviceUnavailableException(String message, DeviceUnavailableException.Type type) {
      super(message);
      this.type = type;
    }

  /**
   * Obtiene el tipo (clasificación) de la excepción.
   *
   * @return el {@link DeviceUnavailableException.Type} asociado a esta excepción
   */
  public DeviceUnavailableException.Type getType() {
    return type;
  }
}
