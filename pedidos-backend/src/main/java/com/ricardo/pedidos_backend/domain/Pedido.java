// Pedido.java
package com.ricardo.pedidos_backend.domain;
import java.time.LocalDateTime;
import java.util.UUID;

public record Pedido(UUID id, String produto, int quantidade, LocalDateTime dataCriacao) {}


