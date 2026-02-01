package com.infragest.infra_orders_service.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO para realizar la solicitud de restauración de estados originales de los dispositivos.
 *
 * {@code items} contiene una lista de {@link RestoreItem}, cada uno representando un dispositivo a restaurar
 * con su estado original.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestoreDevicesRq {

    /**
     * Lista de dispositivos junto con sus estados originales que deben ser restaurados.
     */
    @NotEmpty(message = "La lista de items no puede estar vacía")
    @Valid
    private List<RestoreItem> items;

    /**
     * Sub-clase interna que representa cada dispositivo y su estado original.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RestoreItem {

        /**
         * Identificador único del dispositivo.
         */
        @NotNull(message = "deviceId es requerido")
        private UUID deviceId;

        /**
         * Estado al que debe restaurarse el dispositivo (por ejemplo: GOOD_CONDITION, REGULAR).
         */
        @NotNull(message = "state es requerido")
        private DeviceStatusEnum state;
    }

}
