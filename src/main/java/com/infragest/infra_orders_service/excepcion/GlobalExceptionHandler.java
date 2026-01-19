package com.infragest.infra_orders_service.excepcion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Maneja errores de validación generados por {@code @Valid}.
     *
     * Este manejador intercepta excepciones de validación disparadas en los controladores
     * que usan DTOs anotados con restricciones de validación de Jakarta Bean Validation
     * (por ejemplo, {@code @NotNull}, {@code @Size}, etc.).
     * Devuelve un código HTTP 400 (Bad Request) con información detallada del error.
     *
     * @param ex excepción de validación generada por el framework Spring
     * @return respuesta con detalles del error
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        // Extraer el primer error de validación que ocurrió
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        // Registrar el error en los logs
        log.error("Validation Exception occurred: {}", message);

        // Construir y devolver la respuesta
        return buildErrorResponse("Validation Error", HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Maneja {@link OrderException} y devuelve una respuesta con códigos HTTP apropiados.
     *
     * Las excepciones {@link OrderException} representan errores específicos de negocio
     * relacionados con órdenes, como errores de datos ausentes, conflictos de estados,
     * o faltantes en la base de datos.
     *
     * Mapea el tipo de error (`OrderException.Type`) a códigos de estado HTTP como 400 (Bad Request),
     * 404 (Not Found), o 409 (Conflict).
     *
     * @param ex la excepción que se lanzó al procesar una orden
     * @return ResponseEntity con los detalles del error y el código HTTP adecuado
     */
    @ExceptionHandler(OrderException.class)
    public ResponseEntity<Map<String, Object>> handleOrderException(OrderException ex) {
        // Determinar el código de estado HTTP basado en el tipo de la excepción
        HttpStatus status = switch (ex.getType()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_SERVER -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST; // Otros casos regresan 400 como predeterminado
        };

        // Registrar el error en los logs
        log.error("OrderException occurred: {}", ex.getMessage());

        // Construir y devolver la respuesta
        return buildErrorResponse("Order Error", status, ex.getMessage());
    }

    /**
     * Maneja {@link DeviceUnavailableException}.
     *
     * Esta excepción indica problemas específicos relacionados con los dispositivos,
     * como estados conflictivos (por ejemplo, "NEEDS_REPAIR", "OCCUPIED") o errores
     * en la comunicación con servicios externos relacionados a dispositivos.
     *
     * Mapea el tipo de error (`DeviceUnavailableException.Type`) a códigos de estado HTTP como
     * 400 (Bad Request), 404 (Not Found), 409 (Conflict), o 500 (Internal Server Error).
     *
     * @param ex la excepción generada al procesar dispositivos
     * @return ResponseEntity con los detalles del error y el código HTTP adecuado
     */
    @ExceptionHandler(DeviceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleDeviceUnavailableException(DeviceUnavailableException ex) {
        // Determinar el código de estado HTTP basado en el tipo de error
        HttpStatus status = switch (ex.getType()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_SERVER -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST; // Otros casos predeterminados siguen como BAD_REQUEST
        };

        // Registrar el error en los logs
        log.error("DeviceUnavailableException occurred: {}", ex.getMessage());

        // Construir y devolver la respuesta
        return buildErrorResponse("Device Error", status, ex.getMessage());
    }

    /**
     * Maneja {@link GroupUnavailableExcepction}.
     *
     * Esta excepción indica problemas específicos relacionados con los grupos/empleados,
     * en la comunicación con servicios externos relacionados a dispositivos.
     *
     * Mapea el tipo de error (`GroupUnavailableExcepction.Type`) a códigos de estado HTTP como
     * 400 (Bad Request), 404 (Not Found), 409 (Conflict), o 500 (Internal Server Error).
     *
     * @param ex la excepción generada al consultar grupos o empleados
     * @return ResponseEntity con los detalles del error y el código HTTP adecuado
     */
    @ExceptionHandler(GroupUnavailableExcepction.class)
    public ResponseEntity<Map<String, Object>> handleGroupUnavailableException(GroupUnavailableExcepction ex) {
        // Determinar el código de estado HTTP basado en el tipo de error
        HttpStatus status = switch (ex.getType()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL_SERVER -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST; // Otros casos predeterminados siguen como BAD_REQUEST
        };

        // Registrar el error en los logs
        log.error("GroupUnavailableExcepction occurred: {}", ex.getMessage());

        // Construir y devolver la respuesta
        return buildErrorResponse("Group Error", status, ex.getMessage());
    }

    /**
     * Maneja excepciones no controladas y devuelve 500 (Internal Server Error).
     *
     * Este manejador captura cualquier excepción no definida explícitamente en otros
     * `@ExceptionHandler`, lo que asegura que el sistema tenga un comportamiento predecible
     * en casos de error inesperado.
     *
     * Para facilitar la depuración, genera un ID único del error (UUID) que se registra
     * en los logs y se devuelve al cliente como parte de la respuesta.
     *
     * @param ex excepción no controlada que ocurrió durante el procesamiento
     * @return respuesta genérica con un código HTTP 500 y un identificador único de error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericsException(Exception ex) {

        // Generar un identificador único para rastrear el error en los registros
        String errorId = UUID.randomUUID().toString();

        // Registrar el error junto con el identificador generado
        log.error("Unhandled exception occurred. Error ID: {}. Message: {}", errorId, ex.getMessage(), ex);

        // Construir y devolver una respuesta genérica para el cliente
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", "Internal Server Error",
                        "message", "An unexpected error occurred. Error ID: " + errorId
                ));
    }

    /**
     * Método auxiliar para construir respuestas de error consistentes.
     *
     * Este método centraliza la creación de la estructura de respuesta para excepciones,
     * garantizando que todas las respuestas sigan el mismo formato:
     * `{ timestamp, status, error, message }`.
     *
     * @param error el nombre o tipo del error (por ejemplo, "Validation Error")
     * @param status el código de estado HTTP asociado al error
     * @param message un mensaje descriptivo sobre el error
     * @return ResponseEntity con los detalles del error
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String error, HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", status.value(),
                        "error", error,
                        "message", message
                ));
    }
}
