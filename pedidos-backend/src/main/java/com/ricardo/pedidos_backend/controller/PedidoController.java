package com.ricardo.pedidos_backend.controller;

import com.ricardo.pedidos_backend.domain.Pedido;
import com.ricardo.pedidos_backend.service.PedidoPublisher;
import com.ricardo.pedidos_backend.service.StatusStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {
    private final PedidoPublisher publisher;
    private final StatusStore statusStore;

    public PedidoController(PedidoPublisher publisher, StatusStore statusStore) {
        this.publisher = publisher; this.statusStore = statusStore;
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String,Object> body) {
        String produto = String.valueOf(body.getOrDefault("produto","")).trim();
        int quantidade = Integer.parseInt(String.valueOf(body.getOrDefault("quantidade", "0")));
        if (produto.isEmpty() || quantidade <= 0) {
            return ResponseEntity.badRequest().body(Map.of("erro","produto/quantidade inválidos"));
        }
        UUID id = UUID.randomUUID();
        var pedido = new Pedido(id, produto, quantidade, LocalDateTime.now());
        statusStore.set(id, "RECEBIDO");
        publisher.publicar(pedido);
        return ResponseEntity.accepted().body(Map.of("id", id.toString()));
    }

    @GetMapping("/status/{id}")
    public Map<String,String> status(@PathVariable UUID id) {
        return Map.of("id", id.toString(), "status", statusStore.get(id));
    }
}
