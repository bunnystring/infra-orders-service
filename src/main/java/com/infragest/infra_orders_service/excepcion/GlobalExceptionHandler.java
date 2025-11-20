package com.infragest.infra_orders_service.excepcion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación generados por {@code @Valid}.
     *
     * @param ex excepción de validación
     * @return ResponseEntity con status 400 y mensaje de validación
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        assert message != null;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", HttpStatus.BAD_REQUEST.value(),
                        "error", "Validation Error",
                        "message", message
                ));
    }

    /**
     * Maneja {@link OrderException} y la mapea a un código HTTP.
     *
     * @param ex excepción de negocio del módulo Order
     * @return ResponseEntity con el status apropiado y detalles del error
     */
    @ExceptionHandler(OrderException.class)
    public ResponseEntity<?> handleDeviceException(OrderException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex.getType() == OrderException.Type.NOT_FOUND) {
            status = HttpStatus.NOT_FOUND;
        }
        return ResponseEntity.status(status)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", status.value(),
                        "error", "Order Error",
                        "type", ex.getType(),
                        "message", ex.getMessage()
                ));
    }

    /**
     * Maneja excepciones no controladas y devuelve 500.
     *
     * @param ex excepción no controlada
     * @return ResponseEntity con status 500 y mensaje del error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGenericsException(Exception ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", LocalDateTime.now(),
                        "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "error", "Internal Server Error",
                        "message", ex.getMessage()
                ));
    }

}
