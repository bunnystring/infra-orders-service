package com.infragest.infra_orders_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.UUID;

@FeignClient(name = "infra-groups-service")
public interface EmployeeClient {

    @GetMapping("/employees/{id}")
    Map<String, Object> getEmployee(@PathVariable("id") UUID id);
}
