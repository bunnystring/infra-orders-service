package com.infragest.infra_orders_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO de respuesta para un Group de correos de empleados.
 *
 * @author bunnystring
 * @since 2026-01-18
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMembersEmailRs {

    /**
     * Identificador del grupo consultado.
     */
    private UUID groupId;

    /**
     * Nombre del grupo (opcional, útil para respuestas legibles).
     */
    private String groupName;

    /**
     * Correos electrónicos de los miembros del grupo.
     */
    private Set<String> emails;

    /**
     * Número de correos devueltos (emails.size()).
     */
    private Integer count;

    /**
     * Fecha y hora en que se obtuvo la respuesta.
     */
    private LocalDateTime fetchedAt;

}
