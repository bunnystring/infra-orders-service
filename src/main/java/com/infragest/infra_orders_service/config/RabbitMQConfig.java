package com.infragest.infra_orders_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de RabbitMQ para el microservicio de órdenes.
 *
 * Proporciona la base para que el microservicio se conecte a RabbitMQ
 * y envíe eventos relacionados con órdenes de manera eficiente y en un formato estándar.
 *
 * @author bunnystring
 * @since 2026-01-27
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Nombre del exchange principal para órdenes.
     *
     */
    public static final String EXCHANGE_NAME = "orders.exchange";

    /**
     * Nombre del exchange donde se publican confirmaciones desde `notifications`.
     */
    public static final String NOTIFICATIONS_EXCHANGE_NAME = "notifications.exchange";

    /**
     * Nombre de la cola para recibir confirmaciones de notificaciones.
     */
    public static final String NOTIFICATIONS_QUEUE_NAME = "orders.notifications.queue";


    /**
     * Declara un exchange de tipo Topic llamado "orders.exchange".
     *
     *
     * @return un {@link TopicExchange} con el nombre {@code "orders.exchange"}.
     */
    @Bean
    public TopicExchange ordersExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * Declara un exchange para recibir confirmaciones desde `notifications`.
     *
     * @return un {@link TopicExchange} con el nombre {@code "notifications.exchange"}.
     */
    @Bean
    public TopicExchange notificationsExchange() {
        return new TopicExchange(NOTIFICATIONS_EXCHANGE_NAME);
    }

    /**
     * Declara una cola durable llamada "orders.notifications.queue".
     * Esta cola almacenará eventos de confirmación de notificaciones procesadas.
     *
     * @return una {@link Queue} construida como durable.
     */
    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE_NAME).build();
    }

    /**
     * Vincula la cola "orders.notifications.queue" al exchange "notifications.exchange"
     * con una routing key para manejar confirmaciones exitosas o fallidas.
     *
     * @param notificationsQueue  la cola de confirmaciones.
     * @param notificationsExchange el exchange de confirmaciones.
     * @return un {@link Binding} que conecta la cola al exchange.
     */
    @Bean
    public Binding notificationsQueueBinding(Queue notificationsQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsQueue)
                .to(notificationsExchange)
                .with("notification.completed");
    }

    /**
     * Configura un convertidor de mensajes basado en JSON.
     *
     * @return un {@link Jackson2JsonMessageConverter} para manejar los mensajes en formato JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configura un RabbitTemplate para manejar la comunicación con RabbitMQ.
     *
     * @param connectionFactory la conexión a RabbitMQ, proporcionada por Spring Boot.
     * @param messageConverter el convertidor de mensajes a utilizar (en este caso, JSON).
     * @return un {@link RabbitTemplate} configurado para enviar mensajes a RabbitMQ.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}