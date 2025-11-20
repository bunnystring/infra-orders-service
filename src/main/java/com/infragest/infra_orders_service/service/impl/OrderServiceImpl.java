package com.infragest.infra_orders_service.service.impl;

import com.infragest.infra_orders_service.client.DevicesClient;
import com.infragest.infra_orders_service.entity.Order;
import com.infragest.infra_orders_service.entity.OrderItem;
import com.infragest.infra_orders_service.enums.AssigneeType;
import com.infragest.infra_orders_service.enums.OrderState;
import com.infragest.infra_orders_service.event.OrderEvent;
import com.infragest.infra_orders_service.excepcion.OrderException;
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
   // private final GroupClient groupClient;

    /**
     * Inyección de dependencia: EmployeeClient
     */
   // private final EmployeeClient employeeClient;

    /**
     * Inyección de dependencia: RabbitTemplate
     */
    // private final RabbitTemplate rabbitTemplate;


    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            DevicesClient devicesClient
            // GroupClient groupClient,
            // EmployeeClient employeeClient,
            // RabbitTemplate rabbitTemplate,
        )
    {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.devicesClient = devicesClient;
       // this.groupClient = groupClient;
       // this.employeeClient = employeeClient;
       // this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Transactional
    public OrderRs createOrder(OrderRq rq) {
        if (rq == null) {
            throw new OrderException(MessageException.INVALID_REQUEST,OrderException.Type.BAD_REQUEST);
        }
        if (rq.getDevicesIds() == null || rq.getDevicesIds().isEmpty()) {
            throw new OrderException(MessageException.INVALID_REQUEST,OrderException.Type.BAD_REQUEST);
        }
        if (rq.getAssigneeId() == null || rq.getAssigneeType() == null) {
            throw new OrderException(MessageException.INVALID_REQUEST,OrderException.Type.BAD_REQUEST);
        }

        // 1) Resolver y validar recipients (emails) del assignee
        // List<String> recipients = resolveRecipientsAndValidate(rq.getAssigneeType(), rq.getAssigneeId());

        // 2) Consultar devices-service para obtener estados originales
        List<Map<String, Object>> devices;
        try {
            devices = devicesClient.getDevicesByIds(Map.of("ids", rq.getDevicesIds()));
        } catch (FeignException fe) {
            log.error("Error comunicándose con devices-service: {}", fe.getMessage());
            throw new OrderException(MessageException.DATABASE_ERROR, OrderException.Type.INTERNAL_SERVER);
        }

        if (devices == null || devices.size() != rq.getDevicesIds().size()) {
            throw new OrderException(String.format(MessageException.ORDER_NOT_FOUND, rq.getDevicesIds().toString()),
                    OrderException.Type.NOT_FOUND);
        }

        // 3) Validar estados y recopilar estados originales
        List<UUID> unavailable = new ArrayList<>();
        Map<UUID, String> originalStates = new HashMap<>();
        for (Map<String, Object> d : devices) {
            Object idObj = d.get("id");
            UUID devId;
            try {
                if (idObj instanceof UUID) {
                    devId = (UUID) idObj;
                } else {
                    devId = UUID.fromString(String.valueOf(idObj));
                }
            } catch (Exception ex) {
                log.warn("No se pudo parsear device id: {}", idObj);
                throw new OrderException(String.format(MessageException.INVALID_UUID, String.valueOf(idObj)),
                        OrderException.Type.BAD_REQUEST);
            }
            String state = String.valueOf(d.getOrDefault("status", d.getOrDefault("state", "")));
            originalStates.put(devId, state);
            if ("OCCUPIED".equalsIgnoreCase(state) || "NEEDS_REPAIR".equalsIgnoreCase(state)) {
                unavailable.add(devId);
            }
        }
        if (!unavailable.isEmpty()) {
            throw new OrderException(String.format(MessageException.EQUIPMENT_NOT_AVAILABLE, unavailable.toString()),
                    OrderException.Type.CONFLICT);
        }

        // 4) Reservar devices (marcar OCCUPIED)
        Map<String, Object> reserveBody = Map.of("deviceIds", rq.getDevicesIds(), "state", "OCCUPIED");
        Map<String, Object> reserveResponse;
        try {
            reserveResponse = devicesClient.reserveDevices(reserveBody);
        } catch (FeignException fe) {
            log.error("Error al reservar devices en devices-service: {}", fe.getMessage());
            throw new OrderException(MessageException.DATABASE_ERROR, OrderException.Type.INTERNAL_SERVER);
        }
        Boolean reserved = (Boolean) reserveResponse.getOrDefault("success", Boolean.FALSE);
        if (!Boolean.TRUE.equals(reserved)) {
            throw new OrderException(String.format(MessageException.ORDER_CREATE_FAILED,
                    String.valueOf(reserveResponse.getOrDefault("message", ""))),
                    OrderException.Type.CONFLICT);
        }

        // 5) Persistir Order + OrderItem
        Order order = Order.builder()
                .description(rq.getDescription())
                .state(OrderState.CREATED)
                .assigneeType(rq.getAssigneeType())
                .assigneeId(rq.getAssigneeId())
                .build();

        List<OrderItem> items = rq.getDevicesIds().stream()
                .map(eid -> OrderItem.builder()
                        .deviceId(eid)
                        .originalDeviceState(originalStates.getOrDefault(eid, null))
                        .order(order)
                        .build())
                .collect(Collectors.toList());


        order.setItems(items);
        Order saved = orderRepository.save(order);

        // 6) Publicar evento OrderCreated incluyendo recipientEmails
        OrderEvent event = OrderEvent.builder()
                .orderId(saved.getId())
                .state(saved.getState())
                .description(saved.getDescription())
                .assigneeType(saved.getAssigneeType() != null ? saved.getAssigneeType().name() : null)
                .assigneeId(saved.getAssigneeId())
                .deviceIds(saved.getItems().stream().map(OrderItem::getDeviceId).collect(Collectors.toList()))
              //  .recipientEmails(recipients)
                .build();

      /*  try {
            rabbitTemplate.convertAndSend(RabbitConfig.ORDERS_EXCHANGE, RabbitConfig.ORDERS_ROUTING_KEY_CREATED, event);
        } catch (Exception ex) {
            log.warn("No se pudo publicar evento OrderCreated para order {}: {}", saved.getId(), ex.getMessage());
        }*/

        return toOrderRs(saved);
    }

    /**
     * Lista todas las órdenes.
     *
     * @return lista de {@link OrderRs}
     */
    @Override
    public List<OrderRs> listOrders() {
        return List.of();
    }

    /**
     * Obtiene una orden por su identificador UUID.
     *
     * @param id UUID de la orden
     * @return Optional con {@link OrderRs} si existe, vacío si no existe
     */
    @Override
    public Optional<OrderRs> getOrder(UUID id) {
        return Optional.empty();
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
     * Válida la existencia/estado del assignee y devuelve la lista de emails.
     *
     * @param assigneeType tipo de assignee (GROUP o EMPLOYEE)
     * @param assigneeId   id del assignee
     * @return lista de emails a notificar (uno o varios)
     * @throws OrderException en caso de error de validación o comunicación
     */
    private List<String> resolveRecipientsAndValidate(AssigneeType assigneeType, UUID assigneeId) {
        if (assigneeType == null || assigneeId == null) {
            throw new OrderException(
                    MessageException.INVALID_REQUEST, OrderException.Type.BAD_REQUEST);
        }

       /* try {
            if (assigneeType == AssigneeType.GROUP) {
                // validar existencia del grupo
                Map<String, Object> group = groupClient.getGroup(assigneeId);
                if (group == null || group.isEmpty()) {
                    throw new OrderException(
                            String.format(MessageException.GROUP_NOT_FOUND, assigneeId),
                            OrderException.Type.NOT_FOUND
                    );
                }
                // obtener emails de miembros
                List<String> emails = groupClient.getGroupMembersEmails(assigneeId);
                if (emails == null || emails.isEmpty()) {
                    throw new OrderException(
                            String.format(MessageException.GROUP_NO_MEMBERS, assigneeId),
                            OrderException.Type.CONFLICT
                    );
                }
                return emails;
            } else {
                // assignee es un empleado
                Map<String, Object> emp = employeeClient.getEmployee(assigneeId);
                if (emp == null || emp.isEmpty()) {
                    throw new com.infragest.infra_orders_service.exception.OrderException(
                            String.format(MessageException.EMPLOYEE_NOT_FOUND, assigneeId),
                            com.infragest.infra_orders_service.exception.OrderException.Type.NOT_FOUND
                    );
                }
                String status = String.valueOf(emp.getOrDefault("status", ""));
                if (!"ACTIVE".equalsIgnoreCase(status)) {
                    throw new OrderException(
                            String.format(MessageException.EMPLOYEE_INACTIVE, assigneeId),
                            OrderException.Type.CONFLICT
                    );
                }
                String email = String.valueOf(emp.get("email"));
                if (email == null || email.isBlank() || "null".equalsIgnoreCase(email)) {
                    throw new OrderException(
                            String.format(MessageException.EMPLOYEE_NO_EMAIL, assigneeId),
                            OrderException.Type.CONFLICT
                    );
                }
                return List.of(email);
            }
        } catch (feign.FeignException fe) {
            log.error("Error comunicándose con infra-groups-service para assignee {}: {}", assigneeId, fe.getMessage(), fe);
            throw new com.infragest.infra_orders_service.exception.OrderException(
                    MessageException.DEPENDENCY_ERROR,
                    com.infragest.infra_orders_service.exception.OrderException.Type.INTERNAL_SERVER
            );
        } */

        return null;
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

}
