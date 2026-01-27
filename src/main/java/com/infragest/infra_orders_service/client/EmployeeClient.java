package com.infragest.infra_orders_service.client;

import com.infragest.infra_orders_service.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;


/**
 * Feign client para comunicarse con infra-groups-service.
 *
 * @author bunnystring
 * @since 2026-01-19
 */
@FeignClient(name = "infra-groups-service", contextId = "employeeClient", configuration = FeignClientConfig.class)
public interface EmployeeClient {

    @GetMapping("/api/employees/{id}")
    Map<String, Object> getEmployee(@PathVariable("id") UUID id);
}
