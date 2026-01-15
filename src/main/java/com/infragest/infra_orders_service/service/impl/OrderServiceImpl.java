package com.infragest.infra_orders_service.service.impl;

import com.infragest.infra_orders_service.client.DevicesClient;
import com.infragest.infra_orders_service.client.EmployeeClient;
import com.infragest.infra_orders_service.client.GroupClient;
import com.infragest.infra_orders_service.entity.Order;
import com.infragest.infra_orders_service.entity.OrderItem;
import com.infragest.infra_orders_service.enums.AssigneeType;
import com.infragest.infra_orders_service.enums.OrderState;
import com.infragest.infra_orders_service.event.OrderEvent;
import com.infragest.infra_orders_service.excepcion.DeviceUnavailableException;
import com.infragest.infra_orders_service.excepcion.OrderException;
import com.infragest.infra_orders_service.model.DevicesBatchRq;
import com.infragest.infra_orders_service.model.OrderItemDto;
import com.infragest.infra_orders_service.model.OrderRq;
import com.infragest.infra_orders_service.model.OrderRs;
import com.infragest.infra_orders_service.repository.OrderItemRepository;
import com.infragest.infra_orders_service.repository.OrderRepository;
import com.infragest.infra_orders_service.service.OrderService;
import com.infragest.infra_orders_service.util.MessageException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de órdenes.
 *
 * La clase usa MessageUtils para lanzar excepciones parametrizadas (i18n).
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    /**
     * Inyección de dependencia: OrderRepository
     */
    private final OrderRepository orderRepository;

    /**
     * Inyección de dependencia: OrderItemRepository
     */
    private final OrderItemRepository orderItemRepository;

    /**
     * Inyección de dependencia: DevicesClient
     */
    private final DevicesClient devicesClient;

    /**
     * Inyección de dependencia: GroupClient
     */
    private final GroupClient groupClient;

    /**
     * Inyección de dependencia: EmployeeClient
     */
    private final EmployeeClient employeeClient;

    /**
     * Inyección de dependencia: RabbitTemplate
     */
    // private final RabbitTemplate rabbitTemplate;


    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            DevicesClient devicesClient,
            GroupClient groupClient,
            EmployeeClient employeeClient
            // RabbitTemplate rabbitTemplate,
        )
    {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.devicesClient = devicesClient;
        this.groupClient = groupClient;
        this.employeeClient = employeeClient;
       // this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Transactional
    public OrderRs createOrder(OrderRq rq) {

        // Validar la solicitud
        validateRequest(rq);

        // Verificar dispositivos y obtener su estado original
        Map<UUID, String> originalStates = verifyDevicesAndFetchState(rq.getDevicesIds());

        // Reservar dispositivos
        reserveDevices(rq.getDevicesIds());

        // Crear la orden y guardar los datos
        Order savedOrder = saveOrderAndItems(rq, originalStates);

        List<String> recipients = resolveRecipientsAndValidate(rq.getAssigneeType(), rq.getAssigneeId());

        // Publicar el evento (opcional, elimínalo si no se usa ahora mismo)
        publishOrderCreatedEvent(savedOrder);

        // Mapear y devolver la orden como DTO
        return toOrderRs(savedOrder);
    }

    /**
     * Lista todas las órdenes.
     *
     * @return lista de {@link OrderRs}
     */
    @Override
    public List<OrderRs> listOrders() {

        // Consultar todas las órdenes desde la base de datos
        List<Order> orders = orderRepository.findAll();

        // Mapear entidades Order a DTO OrderRs
        return orders.stream()
                .map(this::toOrderRs) // Convierte cada entidad a su respectivo DTO
                .collect(Collectors.toList());
    }

    /**
     * Obtiene una orden por su identificador UUID.
     *
     * @param id UUID de la orden
     * @return Optional con {@link OrderRs} si existe, vacío si no existe
     */
    @Override
    @Transactional(readOnly = true)
    public OrderRs getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderException(
                        String.format("Orden no encontrada con ID: %s", id),
                        OrderException.Type.NOT_FOUND));
        return toOrderRs(order);
    }

    /**
     * Cambia el estado de una orden y ejecuta las acciones asociadas al cambio.
     * <p>
     * Comportamiento esperado:
     * - Si el nuevo estado es FINISHED, restaurar los estados originales de los equipos en infra-devices-service.
     * - Publicar evento de cambio de estado en RabbitMQ.
     *
     * @param orderId  UUID de la orden
     * @param newState nuevo estado a aplicar
     * @return la representación actualizada {@link OrderRs}
     * @throws RuntimeException si la orden no existe o si la transición no es válida
     */
    @Override
    public OrderRs changeState(UUID orderId, OrderState newState) {
        return null;
    }

    /**
     * Obtiene las órdenes asociadas a un assignee (empleado o grupo).
     *
     * @param assigneeId UUID del assignee
     * @return lista de {@link OrderRs}
     */
    @Override
    public List<OrderRs> findByAssigneeId(UUID assigneeId) {
        return List.of();
    }

    /**
     * Obtiene las órdenes que incluyen un equipmentId dado.
     *
     * @param equipmentId UUID del equipo
     * @return lista de {@link OrderRs}
     */
    @Override
    public List<OrderRs> findByEquipmentId(UUID equipmentId) {
        return List.of();
    }

    /**
     * Valida la existencia/estado del assignee y devuelve la lista de correos electrónicos.
     *
     * @param assigneeType tipo de assignee (`GROUP` o `EMPLOYEE`).
     * @param assigneeId ID del assignee (UUID del grupo o empleado).
     * @return Una lista de correos electrónicos asociados al asignado (uno o varios).
     * @throws OrderException Si ocurren errores de validación, dependencias externas o de negocio.
     */
    private List<String> resolveRecipientsAndValidate(AssigneeType assigneeType, UUID assigneeId) {
        // Validación inicial
        if (assigneeType == null || assigneeId == null) {
            throw new OrderException(MessageException.INVALID_REQUEST, OrderException.Type.BAD_REQUEST);
        }

        try {
            // Validar el tipo de assignee
            if (assigneeType == AssigneeType.GROUP) {
                return validateGroupAndEmails(assigneeId);
            } else if (assigneeType == AssigneeType.EMPLOYEE) {
                return validateEmployeeAndEmail(assigneeId);
            } else {
                throw new OrderException(
                        String.format("El tipo de assignee '%s' no es válido.", assigneeType),
                        OrderException.Type.BAD_REQUEST
                );
            }
        } catch (FeignException fe) {
            log.error("Error comunicándose con infra-groups-service para assignee {}: {}", assigneeId, fe.getMessage(), fe);
            throw new OrderException(
                    MessageException.DEPENDENCY_ERROR,
                    OrderException.Type.INTERNAL_SERVER
            );
        }
    }

    /**
     * Valida la existencia de un grupo y obtiene los correos electrónicos de sus miembros.
     *
     * @param groupId UUID del grupo.
     * @return Una lista de correos electrónicos de los miembros del grupo.
     * @throws OrderException Si el grupo no existe o no tiene miembros con correos válidos.
     */
    private List<String> validateGroupAndEmails(UUID groupId) {
        // Consultar la existencia del grupo
        Map<String, Object> group = groupClient.getGroup(groupId);
        if (group == null || group.isEmpty()) {
            throw new OrderException(
                    String.format(MessageException.GROUP_NOT_FOUND, groupId),
                    OrderException.Type.NOT_FOUND
            );
        }

        // Obtener los correos de los miembros
        List<String> emails = groupClient.getGroupMembersEmails(groupId);
        if (emails == null || emails.isEmpty()) {
            throw new OrderException(
                    String.format(MessageException.GROUP_NO_MEMBERS, groupId),
                    OrderException.Type.CONFLICT
            );
        }

        return emails;
    }

    /**
     * Valida la existencia de un empleado y obtiene su correo electrónico.
     *
     * @param employeeId UUID del empleado.
     * @return Una lista con el correo electrónico del empleado.
     * @throws OrderException Si el empleado no existe o su estado no es válido.
     */
    private List<String> validateEmployeeAndEmail(UUID employeeId) {
        // Consultar la existencia del empleado
        Map<String, Object> employee = employeeClient.getEmployee(employeeId);
        if (employee == null || employee.isEmpty()) {
            throw new OrderException(
                    String.format(MessageException.EMPLOYEE_NOT_FOUND, employeeId),
                    OrderException.Type.NOT_FOUND
            );
        }

        // Validar el estado del empleado
        String status = String.valueOf(employee.get("status"));
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            throw new OrderException(
                    String.format(MessageException.EMPLOYEE_INACTIVE, employeeId),
                    OrderException.Type.CONFLICT
            );
        }

        // Validar el correo electrónico
        String email = String.valueOf(employee.get("email"));
        if (email == null || email.isBlank() || "null".equalsIgnoreCase(email)) {
            throw new OrderException(
                    String.format(MessageException.EMPLOYEE_NO_EMAIL, employeeId),
                    OrderException.Type.CONFLICT
            );
        }

        return List.of(email);
    }

    /**
     * Mapea la entidad {@link com.infragest.infra_orders_service.entity.Order} a {@link com.infragest.infra_orders_service.model.dto.OrderRs}.
     *
     * @param o entidad Order
     * @return DTO OrderRs (puede ser {@code null} si {@code o} es {@code null})
     */
    private OrderRs toOrderRs(Order o) {
        if (o == null) return null;

        List<OrderItemDto> items = o.getItems().stream()
                .map(it -> OrderItemDto.builder()
                        .deviceId(it.getDeviceId())
                        .originalDeviceState(it.getOriginalDeviceState())
                        .build())
                .collect(Collectors.toList());

        return OrderRs.builder()
                .id(o.getId())
                .description(o.getDescription())
                .state(o.getState())
                .assigneeType(o.getAssigneeType())
                .assigneeId(o.getAssigneeId())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .items(items)
                .build();
    }

    private void validateRequest(OrderRq rq) {
        if (rq == null || rq.getDevicesIds() == null || rq.getDevicesIds().isEmpty()) {
            throw new OrderException(MessageException.INVALID_REQUEST, OrderException.Type.BAD_REQUEST);
        }
        if (rq.getAssigneeId() == null || rq.getAssigneeType() == null) {
            throw new OrderException(MessageException.INVALID_REQUEST, OrderException.Type.BAD_REQUEST);
        }
    }

    private Map<UUID, String> parseDeviceResponse(List<Map<String, Object>> devices, List<UUID> unavailable) {
        Map<UUID, String> originalStates = new HashMap<>();
        for (Map<String, Object> d : devices) {
            UUID devId = parseDeviceId(d.get("id"));
            String state = String.valueOf(d.getOrDefault("status", d.getOrDefault("state", "")));
            originalStates.put(devId, state);

            if ("OCCUPIED".equalsIgnoreCase(state) || "NEEDS_REPAIR".equalsIgnoreCase(state)) {
                unavailable.add(devId);
            }
        }
        return originalStates;
    }

    private UUID parseDeviceId(Object idObj) {
        try {
            if (idObj instanceof UUID) {
                return (UUID) idObj;
            } else {
                return UUID.fromString(String.valueOf(idObj));
            }
        } catch (Exception ex) {
            throw new OrderException(String.format(MessageException.INVALID_UUID, String.valueOf(idObj)),
                    OrderException.Type.BAD_REQUEST);
        }
    }

    /**
     * Verifica la existencia de dispositivos por sus ID y recupera sus estados actuales.
     *
     * @param deviceIds una lista de {@link UUID}s que representan los ID de los dispositivos que se deben verificar.
     *                  No debe ser {@code null} ni estar vacía.
     * @return un {@link Map} donde las llaves son los ID de los dispositivos ({@link UUID}) y los valores
     *         son sus estados correspondientes como cadenas de texto ({@link String}). Nunca retorna {@code null}.
     * @throws DeviceUnavailableException si ocurre un error de comunicación con el servicio externo,
     *                                    si el servicio no responde correctamente o si hay dispositivos
     *                                    que no pudieron ser encontrados. El tipo de excepción relacionado
     *                                    se indica en {@link DeviceUnavailableException.Type}.
     */
    public Map<UUID, String> verifyDevicesAndFetchState(List<UUID> deviceIds) {
        List<Map<String, Object>> devices;
        try {

            // Crea un objeto DevicesBatchRq con la lista de ID
            DevicesBatchRq devicesBatchRq = DevicesBatchRq.builder()
                    .ids(deviceIds)
                    .build();

            // Llama al servicio de dispositivos para obtener sus estados
            devices = devicesClient.getDevicesByIds(devicesBatchRq);
        } catch (FeignException fe) {
            log.error("Error comunicándose con el servicio de dispositivos: {}", fe.getMessage());
            throw new DeviceUnavailableException(
                    "Error al comunicarse con el servicio de dispositivos.",
                    DeviceUnavailableException.Type.INTERNAL_SERVER
            );
        }

        // Validar que todos los dispositivos existan en la respuesta
        if (devices == null || devices.size() != deviceIds.size()) {
            throw new DeviceUnavailableException(
                    String.format("Algunos dispositivos no se encontraron: %s", deviceIds),
                    DeviceUnavailableException.Type.NOT_FOUND
            );
        }

        return processDeviceStates(devices);
    }

    private Map<UUID, String> processDeviceStates(List<Map<String, Object>> devices) {
        Map<UUID, String> originalStates = new HashMap<>();
        List<UUID> unavailableDevices = new ArrayList<>();

        for (Map<String, Object> device : devices) {
            UUID deviceId = parseDeviceId(device.get("id"));
            String state = String.valueOf(device.get("status"));

            originalStates.put(deviceId, state);

            // Verificar estados inválidos
            if ("OCCUPIED".equalsIgnoreCase(state) || "NEEDS_REPAIR".equalsIgnoreCase(state)) {
                unavailableDevices.add(deviceId);
            }
        }

        // Lanzar excepción si hay dispositivos no disponibles
        if (!unavailableDevices.isEmpty()) {
            throw new DeviceUnavailableException(
                    String.format("Los dispositivos no están disponibles: %s", unavailableDevices),
                    DeviceUnavailableException.Type.CONFLICT
            );
        }

        return originalStates;
    }

    private void reserveDevices(List<UUID> deviceIds) {
        Map<String, Object> reserveRequest = Map.of("deviceIds", deviceIds, "state", "OCCUPIED");
        try {
            Map<String, Object> response = devicesClient.reserveDevices(reserveRequest);
            Boolean success = (Boolean) response.getOrDefault("success", false);

            if (!success) {
                String message = String.valueOf(response.getOrDefault("message", "No se pudo reservar los dispositivos."));
                throw new DeviceUnavailableException(message, DeviceUnavailableException.Type.CONFLICT);
            }
        } catch (FeignException fe) {
            log.error("Error al reservar dispositivos: {}", fe.getMessage());
            throw new DeviceUnavailableException(
                    "Error al comunicarse con el servicio de dispositivos.",
                    DeviceUnavailableException.Type.INTERNAL_SERVER
            );
        }
    }

    private Order saveOrderAndItems(OrderRq rq, Map<UUID, String> originalStates) {
        // 1. Crear la entidad Order
        Order order = Order.builder()
                .description(rq.getDescription()) // Descripción opcional
                .state(OrderState.CREATED) // El estado inicial es CREATED
                .assigneeType(rq.getAssigneeType()) // Tipo de assignee
                .assigneeId(rq.getAssigneeId()) // ID del assignee
                .build();

        // 2. Crear los items de la orden basados en los deviceIds
        List<OrderItem> items = rq.getDevicesIds().stream()
                .map(deviceId -> OrderItem.builder()
                        .order(order) // Relacionar con la orden
                        .deviceId(deviceId) // ID del dispositivo asociado
                        .originalDeviceState(originalStates.getOrDefault(deviceId, "UNKNOWN")) // Estado original del dispositivo
                        .build()
                )
                .collect(Collectors.toList());

        // 3. Asociar los items creados con la entidad Order
        order.setItems(items);

        // 4. Guardar la entidad Order (con los items) en la base de datos
        return orderRepository.save(order);
    }

    /**
     * Publica un evento OrderCreated con los detalles de la orden creada.
     *
     * @param order La entidad Order recién guardada en la base de datos.
     */
    private void publishOrderCreatedEvent(Order order) {
        // Construir el objeto del evento
        OrderEvent event = OrderEvent.builder()
                .orderId(order.getId())
                .state(order.getState())
                .description(order.getDescription())
                .assigneeType(order.getAssigneeType() != null ? order.getAssigneeType().name() : null)
                .assigneeId(order.getAssigneeId())
                .deviceIds(order.getItems().stream().map(OrderItem::getDeviceId).collect(Collectors.toList()))
                .build();

        try {
            // Enviar el evento a través del RabbitTemplate (u otra dependencia)
            //rabbitTemplate.convertAndSend("orders.exchange", "order.created", event);

            // Loguear el éxito
            log.info("Evento OrderCreated publicado: {}", event);
        } catch (Exception ex) {
            // Manejar fallos de publicación
            log.error("Error al publicar el evento OrderCreated para la orden {}: {}", order.getId(), ex.getMessage(), ex);
        }
    }
}
