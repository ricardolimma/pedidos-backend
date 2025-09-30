package com.ricardo.pedidos_backend.service;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StatusStore {
    private final Map<UUID, String> status = new ConcurrentHashMap<>();
    public void set(UUID id, String s) { status.put(id, s); }
    public String get(UUID id) { return status.getOrDefault(id, "DESCONHECIDO"); }
}
