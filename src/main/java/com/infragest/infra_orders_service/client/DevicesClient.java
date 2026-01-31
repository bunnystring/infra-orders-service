package com.infragest.infra_orders_service.client;

import com.infragest.infra_orders_service.config.FeignClientConfig;
import com.infragest.infra_orders_service.model.ApiResponseDto;
import com.infragest.infra_orders_service.model.DeviceRs;
import com.infragest.infra_orders_service.model.DevicesBatchRq;
import com.infragest.infra_orders_service.model.RestoreDevicesRq;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * Feign client para comunicarse con infra-devices-service.
 *
 * @author bunnystring
 * @since 2025-11-19
 */
@FeignClient(name = "infra-devices-service", configuration = FeignClientConfig.class)
public interface DevicesClient {

    /**
     * Obtiene información de varios equipos por sus IDs.
     *
     * @param body mapa con key "ids" -> List<UUID>
     * @return lista de mapas con la información de cada equipo (id, state, model, etc.)
     */
    @PostMapping("/api/devices/batch")
    List<DeviceRs> getDevicesByIds(@RequestBody DevicesBatchRq devicesBatchRq);

    /**
     * Reserva o actualiza el estado de una lista de devices.
     *
     * @param body ejemplo: { "deviceIds": [...], "state": "OCCUPIED" }
     * @return mapa con keys como "success" (Boolean) y "message" (String)
     */
    @PutMapping("/api/devices/reserve")
    ApiResponseDto<Void> reserveDevices(@RequestBody Map<String, Object> body);

    /**
     * Restaura los estados originales de devices (usado al finalizar una orden).
     *
     * @param restoreDevicesRq solicitud con los dispositivos y estados originales
     * @return respuesta con el estado de la operación
     */
    @PostMapping("/api/devices/restore")
    Map<String, Object> restoreDeviceStates(@RequestBody RestoreDevicesRq restoreDevicesRq);
}
