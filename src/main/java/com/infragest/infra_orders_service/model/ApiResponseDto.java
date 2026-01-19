package com.infragest.infra_orders_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para respuestas estándar de la API.
 *
 * Este DTO puede ser utilizado para devolver respuestas consistentes en diferentes
 * servicios, proporcionando un indicador de éxito, un mensaje, y datos adicionales opcionales.
 *
 * @param <T> El tipo de los datos adicionales incluidos en la respuesta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseDto<T> {

    /**
     * Indicador de éxito de la operación.
     */
    private boolean success;

    /**
     * Descripción o mensaje relacionado con el resultado de la operación.
     */
    private String message;

    /**
     * Datos adicionales opcionales relacionados con la operación.
     */
    private T data;
}
