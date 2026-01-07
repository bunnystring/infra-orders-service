package com.infragest.infra_orders_service.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "infra-groups_service")
public interface GroupClient {


    /**
     * Obtiene información de un grupo por su id.
     *
     * @param id UUID del grupo
     * @return mapa con la información del grupo (id, name, ...) o null
     */
    @GetMapping("/groups/{id}")
    Map<String, Object> getGroup(@PathVariable("id") UUID id);

    /**
     * Obtiene los correos electrónicos de los miembros del grupo.
     *
     * @param id UUID del grupo
     * @return lista de emails (puede ser vacía)
     */
    @GetMapping("/groups/{id}/members/emails")
    List<String> getGroupMembersEmails(@PathVariable("id") UUID id);


    @PostMapping("/groups/{id}/employees")
    Map<String, Object> assignEmployees(@PathVariable("id") UUID id,
                                        @RequestBody Map<String, Object> body);

}
