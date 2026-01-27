package com.infragest.infra_orders_service.enums;

public enum NotificationStatus {

    PENDING,  // Estado predeterminado para nuevas órdenes.
    SENT,     // Notificación enviada con éxito.
    FAILED    // Ocurrió un error al enviar la notificación.

}
