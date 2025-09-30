package com.ricardo.pedidos_backend.service;

import com.ricardo.pedidos_backend.domain.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.env.Environment;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class PedidoPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private Environment env;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        env = mock(Environment.class);
    }

    @Test
    void publicar_enviaParaDefaultExchange_comRoutingKeyDaConfig() {

        when(env.getProperty("app.queues.entrada")).thenReturn("q.pedidos");
        PedidoPublisher publisher = new PedidoPublisher(rabbitTemplate, env);

        Pedido pedido = mock(Pedido.class);

        publisher.publicar(pedido);

        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(""), eq("q.pedidos"), same(pedido));
        verifyNoMoreInteractions(rabbitTemplate);
    }

    @Test
    void publicar_quandoPropertyAusente_aRoutingKeyFicaNull_masMetodoAindaEhChamado() {
        when(env.getProperty("app.queues.entrada")).thenReturn(null);
        PedidoPublisher publisher = new PedidoPublisher(rabbitTemplate, env);
        Pedido pedido = mock(Pedido.class);

        publisher.publicar(pedido);

        verify(rabbitTemplate, times(1))
                .convertAndSend(eq(""), isNull(), same(pedido));
        verifyNoMoreInteractions(rabbitTemplate);
    }
}
