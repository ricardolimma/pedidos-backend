package com.ricardo.pedidos_backend.messaging;

import com.ricardo.pedidos_backend.domain.Pedido;
import com.ricardo.pedidos_backend.domain.StatusPedido;
import com.ricardo.pedidos_backend.service.StatusStore;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
public class PedidoConsumer {
    private final StatusStore statusStore;
    private final RabbitTemplate rabbit;
    private final String sucessoQ, falhaQ;

    public PedidoConsumer(StatusStore store, RabbitTemplate rabbit, Environment env) {
        this.statusStore = store; this.rabbit = rabbit;
        this.sucessoQ = env.getProperty("app.queues.statusSucesso");
        this.falhaQ = env.getProperty("app.queues.statusFalha");
    }

    @RabbitListener(queues = "#{entradaQueue.name}")
    public void onMessage(Pedido p) throws InterruptedException {
        statusStore.set(p.id(), "PROCESSANDO");
        Thread.sleep(ThreadLocalRandom.current().nextLong(1000, 3001));

        double r = ThreadLocalRandom.current().nextDouble();
        if (r < 0.2) {
            // publica falha e rejeita para DLQ
            rabbit.convertAndSend("", falhaQ, StatusPedido.falha(p.id(), "ExcecaoDeProcessamento"));
            statusStore.set(p.id(), "FALHA");
            throw new AmqpRejectAndDontRequeueException("Falha simulada");
        } else {
            rabbit.convertAndSend("", sucessoQ, StatusPedido.sucesso(p.id()));
            statusStore.set(p.id(), "SUCESSO");
        }
    }
}
