package com.infragest.infra_orders_service.model;


import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DevicesBatchRq {

    /**
     * Lista de ids de equipos a recuperar.
     */
    @NotEmpty(message = "La lista de ids no puede estar vacía")
    private List<UUID> ids;

}