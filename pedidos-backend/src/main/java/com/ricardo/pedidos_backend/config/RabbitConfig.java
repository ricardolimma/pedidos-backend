package com.ricardo.pedidos_backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class RabbitConfig {

    @Value("${app.queues.entrada}") String entrada;
    @Value("${app.queues.entradaDlq}") String entradaDlq;
    @Value("${app.queues.statusSucesso}") String statusSucesso;
    @Value("${app.queues.statusFalha}") String statusFalha;

    @Bean Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean public Queue entradaQueue() {
        return QueueBuilder.durable(entrada)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", entradaDlq)
                .build();
    }
    @Bean public Queue entradaDlqQueue() { return QueueBuilder.durable(entradaDlq).build(); }
    @Bean public Queue statusSucessoQueue() { return QueueBuilder.durable(statusSucesso).build(); }
    @Bean public Queue statusFalhaQueue() { return QueueBuilder.durable(statusFalha).build(); }
}
