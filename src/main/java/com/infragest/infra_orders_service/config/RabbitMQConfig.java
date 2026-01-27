package com.infragest.infra_orders_service.config;

import org.springframework.amqp.core.TopicExchange;
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