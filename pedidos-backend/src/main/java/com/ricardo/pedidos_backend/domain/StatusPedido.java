// StatusPedido.java
package com.ricardo.pedidos_backend.domain;
import java.time.LocalDateTime;
import java.util.UUID;

public record StatusPedido(UUID idPedido, String status, String mensagemErro, LocalDateTime dataProcessamento) {
    public static StatusPedido sucesso(UUID id) {
        return new StatusPedido(id, "SUCESSO", null, LocalDateTime.now());
    }
    public static StatusPedido falha(UUID id, String erro) {
        return new StatusPedido(id, "FALHA", erro, LocalDateTime.now());
    }
}