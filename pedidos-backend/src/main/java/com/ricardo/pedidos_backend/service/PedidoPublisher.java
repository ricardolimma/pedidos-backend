// PedidoPublisher.java
package com.ricardo.pedidos_backend.service;

import com.ricardo.pedidos_backend.domain.Pedido;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PedidoPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final String entradaQueue;

    public PedidoPublisher(RabbitTemplate rabbitTemplate, org.springframework.core.env.Environment env) {
        this.rabbitTemplate = rabbitTemplate;
        this.entradaQueue = env.getProperty("app.queues.entrada");
    }

    public void publicar(Pedido pedido) {
        rabbitTemplate.convertAndSend("", entradaQueue, pedido); // default exchange
    }
}
