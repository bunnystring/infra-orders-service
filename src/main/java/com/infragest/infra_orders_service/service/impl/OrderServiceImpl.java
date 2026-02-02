package com.infragest.infra_orders_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.infragest.infra_orders_service.client.DevicesClient;
import com.infragest.infra_orders_service.client.EmployeeClient;
import com.infragest.infra_orders_service.client.GroupClient;
import com.infragest.infra_orders_service.config.RabbitMQConfig;
import com.infragest.infra_orders_service.entity.Order;
import com.infragest.infra_orders_service.entity.OrderItem;
import com.infragest.infra_orders_service.enums.AssigneeType;
import com.infragest.infra_orders_service.enums.NotificationStatus;
import com.infragest.infra_orders_service.enums.OrderState;
import com.infragest.infra_orders_service.event.NotificationEvent;
import com.infragest.infra_orders_service.event.OrderEvent;
import com.infragest.infra_orders_service.excepcion.DeviceUnavailableException;
import com.infragest.infra_orders_service.excepcion.GroupUnavailableExcepction;
import com.infragest.infra_orders_service.excepcion.OrderException;
import com.infragest.infra_orders_service.model.*;
import com.infragest.infra_orders_service.repository.OrderItemRepository;
import com.infragest.infra_orders_service.repository.OrderRepository;
import com.infragest.infra_orders_service.service.OrderService;
import com.infragest.infra_orders_service.util.MessageException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final RabbitTemplate rabbitTemplate;

    /**
     * Constructor con los parametros iniciales.
     *
     * @param orderRepository
     * @param orderItemRepository
     * @param devicesClient
     * @param groupClient
     * @param employeeClient
     * @param rabbitTemplate
     */
    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            DevicesClient devicesClient,
            GroupClient groupClient,
            EmployeeClient employeeClient,
            RabbitTemplate rabbitTemplate
        )
    {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.devicesClient = devicesClient;
        this.groupClient = groupClient;
        this.employeeClient = employeeClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Crea una nueva orden a partir de la solicitud proporcionada.
     *
     * @param rq Objeto de solicitud que contiene la información necesaria para crear la orden.
     *           Incluye dispositivos, tipos de asignación (`assigneeType`), e identificadores de asignación (`assigneeId`).
     * @return DTO que representa la orden creada.
     * @throws OrderException Si ocurre un error durante la verificación de dispositivos, la validación de destinatarios o la creación de la orden.
     * @since 2026-01-28
     * @author Bunnystring
     */
    @Override
    @Transactional
    public OrderRs createOrder(OrderRq rq) {

        // Verificar dispositivos y obtener su estado original
        Map<UUID, String> originalStates = verifyDevicesAndFetchState(rq.getDevicesIds());

        // Crear la orden y guardar los datos
        Order savedOrder = saveOrderAndItems(rq, originalStates);

        // Reservar dispositivos
        reserveDevices(rq.getDevicesIds(), savedOrder.getId());

        // Obtener los correos asociados a la asignación
        List<String> recipients = resolveRecipientsAndValidate(rq.getAssigneeType(), rq.getAssigneeId());

        // Publicar el evento
        publishOrderEvent(savedOrder, recipients);

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
                .map(this::toOrderRs)
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
                        String.format(MessageException.ORDER_NOT_FOUND, id),
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
    @Transactional
    @Override
    public void changeState(UUID orderId, OrderState newState) {

        // Validar existencia de la orden
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(
                        String.format(MessageException.ORDER_NOT_FOUND, orderId),
                        OrderException.Type.NOT_FOUND
                ));

        // Validar la existencia de los ítems asociados a la orden
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        if (orderItems.isEmpty()) {
            throw new OrderException(
                    String.format(MessageException.INVALID_EQUIPMENT_LIST, orderId),
                    OrderException.Type.BAD_REQUEST
            );
        }

        // Validar transición de estados
        validateStateTransition(order.getState(), newState, orderId);

        // Aplicar el nuevo estado
        order.setState(newState);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        // Ejecutar acciones específicas dependiendo del nuevo estado
        performStateSpecificActions(order, newState);

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
            List<String> recipients;

            // Validar el tipo de assignee
            if (assigneeType == AssigneeType.GROUP) {
                recipients = validateGroupAndEmails(assigneeId);
            } else if (assigneeType == AssigneeType.EMPLOYEE) {
                recipients = validateEmployeeAndEmail(assigneeId);
            } else {
                throw new OrderException(
                        String.format(MessageException.ASSIGNEE_INVALID, assigneeType),
                        OrderException.Type.BAD_REQUEST
                );
            }

            // Verificar que la lista de correos no sea nula ni vacía
            if (recipients == null || recipients.isEmpty()) {
                throw new OrderException(
                        String.format(MessageException.ASSIGNEE_NOT_FOUND,
                                assigneeId, assigneeType),
                        OrderException.Type.NOT_FOUND
                );
            }

            return recipients;
        } catch (FeignException.ServiceUnavailable fe) {
            log.error("Error comunicándose con infra-groups-service para assignee {}: {}", assigneeId, fe.getMessage(), fe);
            throw new OrderException(
                    MessageException.DEPENDENCY_ERROR,
                    OrderException.Type.INTERNAL_SERVER
            );
        } catch (FeignException fe) {
            log.error("Error al obtener los correos: {}", fe.getMessage());
            throw new DeviceUnavailableException(
                    extractFeignErrorMessage(fe, MessageException.DEPENDENCY_ERROR),
                    DeviceUnavailableException.Type.INTERNAL_SERVER
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

        // Validar que groupId no sea null
        if (groupId == null) {
            throw new OrderException(
                    String.format(MessageException.MISSING_PARAMETER, "groupId"),
                    OrderException.Type.BAD_REQUEST
            );
        }

        Map<String, Object> group;

        try {

            // Consultar la existencia del grupo a través del cliente Feign
           group = groupClient.getGroup(groupId);

        } catch (FeignException.ServiceUnavailable fe) {
            log.error("Error comunicándose con el servicio de grupos: {}", fe.getMessage());
            throw new GroupUnavailableExcepction(
                    MessageException.SERVICE_UNAVAILABLE,
                    GroupUnavailableExcepction.Type.INTERNAL_SERVER
            );
        } catch (FeignException fe) {
            log.error("Error inesperado comunicándose con el servicio de grupos: {}", fe.getMessage());
            throw new GroupUnavailableExcepction(
                    MessageException.DEPENDENCY_ERROR,
                    GroupUnavailableExcepction.Type.INTERNAL_SERVER
            );
        }

        if (group == null || group.isEmpty()) {
            throw new OrderException(
                    String.format(MessageException.GROUP_NOT_FOUND, groupId),
                    OrderException.Type.NOT_FOUND
            );
        }

        List<String> emails;

        try {
            // Obtener los correos de los miembros desde el servicio de grupos
            emails = groupClient.getGroupMembersEmails(groupId);

        } catch (FeignException.ServiceUnavailable fe) {
            // Loguea el error de comunicación con el microservicio de grupos
            log.error("Error comunicándose con el servicio de grupos al obtener miembros: {}", fe.getMessage());
            // Lanza una excepción personalizada en caso de error
            throw new GroupUnavailableExcepction(
                    MessageException.SERVICE_UNAVAILABLE,
                    GroupUnavailableExcepction.Type.INTERNAL_SERVER
            );
        } catch (FeignException fe) {
            log.error("Inesperado error comunicándose con el servicio de grupos al obtener miembros {}: {}", groupId, fe.getMessage());
            throw new GroupUnavailableExcepction(
                    MessageException.DEPENDENCY_ERROR,
                    GroupUnavailableExcepction.Type.INTERNAL_SERVER
            );
        }

        if (emails == null || emails.isEmpty()) {
            throw new OrderException(
                    String.format(MessageException.GROUP_NO_MEMBERS, groupId),
                    OrderException.Type.CONFLICT
            );
        }

        return emails;
    }

    /**
     * Válida la existencia de un empleado y obtiene su correo electrónico.
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
        List<DeviceRs> devices;
        try {

            // Crea un objeto DevicesBatchRq con la lista de ID
            DevicesBatchRq devicesBatchRq = DevicesBatchRq.builder()
                    .ids(deviceIds)
                    .build();

            // Llama al servicio de dispositivos para obtener sus estados
            devices = devicesClient.getDevicesByIds(devicesBatchRq);

        } catch (FeignException.ServiceUnavailable fe) {
            log.error("El servicio de dispositivos no está disponible: {}", fe.getMessage());
            throw new DeviceUnavailableException(
                    MessageException.SERVICE_UNAVAILABLE,
                    DeviceUnavailableException.Type.SERVICE_UNAVAILABLE
            );

        } catch (FeignException.BadRequest fe) {
            log.error("Error en la solicitud al servicio de dispositivos: {}", fe.getMessage());
            throw new DeviceUnavailableException(
                    MessageException.INVALID_REQUEST,
                    DeviceUnavailableException.Type.BAD_REQUEST
            );

        } catch (FeignException fe) {
            log.error("Error de comunicación con el servicio de dispositivos: {}", fe.getMessage());
            // Extraer detalles del mensaje de error del cuerpo de la respuesta
            throw new DeviceUnavailableException(
                    extractFeignErrorMessage(fe, MessageException.DEVICE_ERROR_COMMUNICATION),
                    DeviceUnavailableException.Type.INTERNAL_SERVER
            );
        }

        // Validar que todos los dispositivos existan en la respuesta
        if (devices == null || devices.size() != deviceIds.size()) {
            throw new DeviceUnavailableException(
                    String.format(MessageException.DEVICE_NOT_FOUND_BY_IDS,  deviceIds),
                    DeviceUnavailableException.Type.NOT_FOUND
            );
        }

        return processDeviceStates(devices);
    }

    private Map<UUID, String> processDeviceStates(List<DeviceRs> devices) {
        Map<UUID, String> originalStates = new HashMap<>();
        List<UUID> unavailableDevices = new ArrayList<>();

        for (DeviceRs device : devices) {
            UUID deviceId = parseDeviceId(device.getId());
            String state = String.valueOf(device.getStatus());

            originalStates.put(deviceId, state);

            // Verificar estados inválidos
            if ("OCCUPIED".equalsIgnoreCase(state) || "NEEDS_REPAIR".equalsIgnoreCase(state)) {
                unavailableDevices.add(deviceId);
            }
        }

        // Lanzar excepción si hay dispositivos no disponibles
        if (!unavailableDevices.isEmpty()) {
            throw new DeviceUnavailableException(
                    String.format(MessageException.EQUIPMENT_NOT_AVAILABLE, unavailableDevices),
                    DeviceUnavailableException.Type.CONFLICT
            );
        }

        return originalStates;
    }

    /**
     * Reserva dispositivo mediante una llamada al servicio de dispositivos.
     *
     * @param deviceIds Una lista de identificadores únicos ({@link UUID}) de los dispositivos
     *                  que se deben reservar. La lista no debe ser {@code null} ni estar vacía.
     * @throws DeviceUnavailableException La respuesta del servicio indica que los dispositivos no pudieron ser reservados.
     * Ocurre un error de comunicación con el servicio `devices`.
     */
    private void reserveDevices(List<UUID> deviceIds, UUID orderId) {
        Map<String, Object> reserveRequest = Map.of("deviceIds", deviceIds, "state", "OCCUPIED","orderId", orderId);
        try {
            ApiResponseDto<Void> response = devicesClient.reserveDevices(reserveRequest);

            // Verifica el éxito de la operación
            if (!response.isSuccess()) {
                throw new DeviceUnavailableException(response.getMessage(), DeviceUnavailableException.Type.CONFLICT);
            }

        } catch (FeignException.ServiceUnavailable ex) {
            // Si el microservicio devuelve un 503
            log.error("El servicio de dispositivos no está disponible: {}", ex.getMessage());
            throw new DeviceUnavailableException(
                    MessageException.SERVICE_UNAVAILABLE,
                    DeviceUnavailableException.Type.SERVICE_UNAVAILABLE
            );
        } catch (FeignException fe) {
            log.error("Error al reservar dispositivos: {}", fe.getMessage());
            throw new DeviceUnavailableException(
                    extractFeignErrorMessage(fe, MessageException.DEVICE_ERROR_COMMUNICATION),
                    DeviceUnavailableException.Type.INTERNAL_SERVER
            );
        }
    }

    /**
     * Crea y guarda una nueva orden junto con sus elementos (items) en la base de datos.
     *
     * @param rq La solicitud de creación de la orden, que incluye la descripción, el tipo de asignado
     *           ({@code assigneeType}), el identificador del asignado ({@code assigneeId}) y la lista de IDs de dispositivos.
     * @param originalStates Un mapa donde las llaves son los IDs de los dispositivos asociados a la
     *                       orden y los valores son los estados originales de esos dispositivos.
     *                       Si un dispositivo no tiene un estado en este mapa, se usará "UNKNOWN" como valor predeterminado.
     * @return La entidad {@link Order} recién creada y persistida en la base de datos.
     */
    private Order saveOrderAndItems(OrderRq rq, Map<UUID, String> originalStates) {

        // Crear la entidad Order
        Order order = Order.builder()
                .description(rq.getDescription())
                .state(OrderState.CREATED)
                .assigneeType(rq.getAssigneeType())
                .assigneeId(rq.getAssigneeId())
                .build();

        // Crear los items de la orden basados en los deviceIds
        List<OrderItem> items = rq.getDevicesIds().stream()
                .map(deviceId -> OrderItem.builder()
                        .order(order)
                        .deviceId(deviceId)
                        .originalDeviceState(originalStates.getOrDefault(deviceId, "UNKNOWN")) // Estado original del dispositivo
                        .build()
                )
                .collect(Collectors.toList());

        // Asociar los items creados con la entidad Order
        order.setItems(items);

        // Guardar la entidad Order (con los items) en la base de datos
        return orderRepository.save(order);
    }

    /**
     * Publica un evento OrderCreated con los detalles de la orden creada.
     *
     * @param order La entidad Order recién guardada en la base de datos.
     */
    private void publishOrderEvent(Order order, List<String> recipients) {

        try {
            // Construcción del evento
            OrderEvent event = OrderEvent.builder()
                    .orderId(order.getId())
                    .state(order.getState())
                    .description(order.getDescription())
                    .assigneeType(order.getAssigneeType() != null ? order.getAssigneeType().name() : null)
                    .assigneeId(order.getAssigneeId())
                    .deviceIds(order.getItems().stream()
                            .map(OrderItem::getDeviceId)
                            .collect(Collectors.toList()))
                    .recipientEmails(recipients)
                    .build();

            // Publicar en RabbitMQ
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_NAME,
                    String.format("order.state.%s", order.getState().name().toLowerCase()),
                    event
            );

            log.info("Evento publicado exitosamente. Orden ID: {}, Estado: {}, Routing Key: {}",
                    order.getId(), order.getState(), String.format("order.state.%s", order.getState().name().toLowerCase()));
        } catch (Exception ex) {
            log.error("Error al publicar el evento. Orden ID: {}, Routing Key: {}. Detalle: {}",
                    order.getId(), String.format("order.state.%s", order.getState().name().toLowerCase()), ex.getMessage(), ex);
        }
    }

    /**
     * Lógica para actualizar el estado de la notificación en las órdenes.
     *
     * @param notificationEvent evento que contiene los detalles de la confirmación.
     */
    public void updateOrderNotificationStatus(NotificationEvent notificationEvent) {
        log.info("Actualizando estado de notificación para la orden ID: {}, Estado: {}",
                notificationEvent.getOrderId(),
                notificationEvent.getStatus());

        Optional<Order> optionalOrder = orderRepository.findById(notificationEvent.getOrderId());

        if (optionalOrder.isEmpty()) {
            log.warn("Orden no encontrada para el evento de notificación ID: {}", notificationEvent.getOrderId());
            return;
        }

        Order order = optionalOrder.get();

        // Actualizar el estado de la notificación
        NotificationStatus status = mapNotificationStatus(notificationEvent.getStatus());

        order.setNotificationStatus(status);
        orderRepository.save(order);
        log.info("Estado de la notificación actualizado para la orden ID {}: {}", order.getId(), status);
    }

    /**
     * Convierte el estado recibido como cadena (String) en un valor enumerado ({@link NotificationStatus}).
     *
     * @param status el estado recibido como cadena (generalmente desde un evento).
     *               Este valor no debe ser {@code null} y se espera que sea insensible a mayúsculas/minúsculas.
     * @return el valor correspondiente de {@link NotificationStatus}, o {@code NotificationStatus.PENDING}
     *         si el estado proporcionado no es reconocido.
     */
    private NotificationStatus mapNotificationStatus(String status) {
        switch (status.toUpperCase()) {
            case "SUCCESS":
                return NotificationStatus.SENT;
            case "FAILED":
                return NotificationStatus.FAILED;
            default:
                log.warn("Estado de notificación desconocido: {}", status);
                return NotificationStatus.PENDING;
        }
    }

    /**
     * Extrae el mensaje detallado de error del cuerpo de un FeignException.
     *
     * @param fe El FeignException capturado.
     * @return El mensaje de error extraído del cuerpo de la excepción, o un mensaje genérico si no se puede procesar.
     */
    private String extractFeignErrorMessage(FeignException fe, String defaultMessage) {
        try {
            String responseBody = fe.contentUTF8();
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> errorResponse = objectMapper.readValue(responseBody, Map.class);
            return (String) errorResponse.getOrDefault("message", defaultMessage);
        } catch (Exception ex) {
            return defaultMessage;
        }
    }

    /**
     * Válida que la transición de estados sea permitida según las reglas definidas.
     *
     * @param currentState Estado actual de la orden.
     * @param newState     Nuevo estado al que se desea actualizar la orden.
     * @param orderId      UUID de la orden afectada (para contexto en mensajes de error).
     * @throws OrderException si la transición no es válida.
     */
    private void validateStateTransition(OrderState currentState, OrderState newState, UUID orderId) {
        if (currentState == OrderState.CREATED && newState == OrderState.IN_PROCESS) {
            return;
        } else if (currentState == OrderState.IN_PROCESS && newState == OrderState.DISPATCHED) {
            return;
        } else if (currentState == OrderState.DISPATCHED && newState == OrderState.FINISHED) {
            return;
        }
        throw new OrderException(
                String.format(MessageException.ORDER_STATE_TRANSITION_INVALID, orderId, currentState + " -> " + newState),
                OrderException.Type.BAD_REQUEST
        );
    }

    /**
     * Libera los dispositivos asociados a una orden y restaura sus estados originales.
     *
     * Construye la solicitud de restauración basada en los dispositivos asociados
     * a la orden y sus estados originales, y la envía al servicio de dispositivos.
     *
     * @param order La orden que contiene los dispositivos a liberar.
     * @throws DeviceUnavailableException si ocurre un error al comunicarse con el servicio de dispositivos.
     */
    private void releaseOrderDevices(Order order) {

        // Construir la lista de items necesarios para el DTO del request
        List<RestoreDevicesRq.RestoreItem> restoreItems = order.getItems().stream()
                .map(item -> RestoreDevicesRq.RestoreItem.builder()
                        .deviceId(item.getDeviceId())
                        .state(DeviceStatusEnum.valueOf(item.getOriginalDeviceState()))
                        .build())
                .toList();

        // Validar que haya elementos en la lista antes de continuar
        if (restoreItems.isEmpty()) {
            log.warn("No hay dispositivos asociados para restaurar en la orden {}", order.getId());
            throw new OrderException(
                    String.format(MessageException.INVALID_EQUIPMENT_LIST, order.getId()),
                    OrderException.Type.BAD_REQUEST
            );
        }

        // Crear la solicitud DTO para enviar al endpoint de restore
        RestoreDevicesRq restoreDevicesRq = RestoreDevicesRq.builder()
                .items(restoreItems)
                .build();

        log.info("Ejecutando la restauración de dispositivos para la orden {}: {}", order.getId(), restoreDevicesRq);

        try {
            // Llamar al cliente Feign para restaurar los dispositivos
            Map<String, Object> response = devicesClient.restoreDeviceStates(restoreDevicesRq);

            // Validar la respuesta del servicio
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                String message = (String) response.getOrDefault("message", "La restauración de dispositivos falló sin detalles.");
                throw new DeviceUnavailableException(
                        String.format("Fallo en la restauración de dispositivos para la orden %s: %s", order.getId(), message),
                        DeviceUnavailableException.Type.INTERNAL_SERVER
                );
            }

            log.info("Estados originales restaurados para todos los dispositivos de la orden {}", order.getId());

        } catch (FeignException.ServiceUnavailable fe) {
            log.error("El servicio de dispositivos no está disponible para la orden {}: {}", order.getId(), fe.getMessage());
            throw new DeviceUnavailableException(
                    MessageException.SERVICE_UNAVAILABLE,
                    DeviceUnavailableException.Type.SERVICE_UNAVAILABLE
            );

        } catch (FeignException.BadRequest fe) {
            log.error("Solicitud inválida al servicio de dispositivos para la orden {}: {}", order.getId(), fe.getMessage());
            throw new DeviceUnavailableException(
                    MessageException.INVALID_REQUEST,
                    DeviceUnavailableException.Type.BAD_REQUEST
            );

        } catch (FeignException fe) {
            log.error("Error de comunicación con el servicio de dispositivos para la orden {}: {}", order.getId(), fe.getMessage());
            throw new DeviceUnavailableException(
                    MessageException.DEVICE_ERROR_COMMUNICATION,
                    DeviceUnavailableException.Type.INTERNAL_SERVER
            );
        }
    }

    /**
     * Ejecuta las acciones específicas para un cambio de estado de una orden.
     *
     * Comportamiento:
     * - Para todos los estados, notifica el cambio a RabbitMQ.
     * - Para el estado {@link OrderState#FINISHED}, libera los dispositivos asociados a la orden.
     *
     * @param order   La entidad {@link Order} afectada.
     * @param newState El nuevo estado al que se cambia la orden ({@link OrderState}).
     */
    private void performStateSpecificActions(Order order, OrderState newState) {

        // Obtener los correos asociados a la asignación
        List<String> recipients = resolveRecipientsAndValidate(order.getAssigneeType(), order.getAssigneeId());

        // Notificar a RabbitMQ siempre que haya un cambio de estado
        publishOrderEvent(order, recipients);

        // Ejecución de acciones específicas solo para el estado FINISHED
        if (newState == OrderState.FINISHED) {
            handleFinishedState(order);
        }
    }


    /**
     * Maneja las acciones específicas para el estado {@link OrderState#FINISHED}.
     *
     * Comportamiento:
     * - Libera los dispositivos asociados a la orden y restaura sus estados originales.
     *
     * @param order La entidad {@link Order} cuyo estado está cambiando a {@link OrderState#FINISHED}.
     * @throws OrderException si ocurre un error en el proceso de liberación de dispositivos.
     */
    private void handleFinishedState(Order order) {
        try {

            // Liberar dispositivos
            releaseOrderDevices(order);
            log.info("Se liberaron los dispositivos de la orden {}", order.getId());

        } catch (Exception ex) {
            log.error("Error al liberar dispositivos de la orden {}: {}", order.getId(), ex.getMessage());
            throw new OrderException(
                    String.format(MessageException.EQUIPMENT_RESTORE_FAILED, order.getId()),
                    OrderException.Type.INTERNAL_SERVER
            );
        }
    }
}
