package com.infragest.infra_orders_service.controller;

import com.infragest.infra_orders_service.model.OrderRq;
import com.infragest.infra_orders_service.model.OrderRs;
import com.infragest.infra_orders_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controlador REST para la gestión de órdenes.
 *
 * Este controlador proporciona endpoints para realizar operaciones CRUD
 * básicas en órdenes, como crear nuevas órdenes, listar todas las órdenes
 * existentes y obtener información de una orden específica por su ID.
 *
 * Cada endpoint está documentado con anotaciones OpenAPI para su integración
 * con Swagger u otras herramientas de documentación API.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    /**
     * Constructor que inyecta el servicio de órdenes.
     *
     * @param orderService Servicio que contiene la lógica de negocio para Órdenes
     */
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Crear una nueva orden.
     *
     * Este endpoint permite crear órdenes con una lista de dispositivos
     * y un grupo o empleado asignado. La solicitud debe incluir los datos de
     * la orden en el cuerpo de la petición (request body) como un objeto `OrderRq`.
     *
     * @param orderRequest Solicitud que contiene los datos de la orden a crear (en formato JSON)
     * @return El objeto `OrderRs` que representa la orden creada, junto con el código HTTP 201 (CREATED).
     */
    @Operation(summary = "Crear una orden", description = "Crea una orden con dispositivos, empleado o grupo asignado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Orden creada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderRs.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud inválida (error en la validación o dispositivos/empleados inexistentes)",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Error en la creación de la orden",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<OrderRs> createOrder(@Valid @RequestBody OrderRq orderRequest) {
        return ResponseEntity.status(201).body(orderService.createOrder(orderRequest));
    }

    /**
     * Listar todas las órdenes.
     *
     * Este endpoint devuelve una lista de todas las órdenes disponibles en el sistema.
     * Si no hay órdenes registradas, devuelve una lista vacía.
     *
     * @return Una lista de objetos `OrderRs` que representan las órdenes registradas.
     * Si no hay órdenes disponibles, la respuesta será una lista vacía con un código HTTP 200 (OK).
     */
    @Operation(summary = "Listar todas las órdenes", description = "Devuelve una lista de todas las órdenes registradas en el sistema.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de órdenes encontrada",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderRs.class))),
            @ApiResponse(responseCode = "204", description = "No hay órdenes registradas",
                    content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<OrderRs>> listAllOrders() {
        return ResponseEntity.ok(orderService.listOrders());
    }

    /**
     * Obtener una orden por su identificador único.
     *
     * Este endpoint permite consultar los detalles de una orden usando su UUID.
     * Si la orden no existe, el manejador global de excepciones (GlobalExceptionHandler)
     * devolverá una respuesta HTTP 404 (Not Found).
     *
     * @param id UUID único de la orden a recuperar.
     * @return El objeto `OrderRs` que representa la orden encontrada, junto con el código HTTP 200 (OK).
     */
    @Operation(summary = "Obtener una orden por su ID", description = "Devuelve los detalles de una orden según su identificador único.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orden encontrada con éxito",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderRs.class))),
            @ApiResponse(responseCode = "404", description = "La orden no fue encontrada", content = @Content),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos en la solicitud", content = @Content),
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrderRs> getOrderById(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }
}
