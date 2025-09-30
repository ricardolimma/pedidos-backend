# Backend — Pedidos (Spring Boot)

API responsável por **receber pedidos**, responder `202 Accepted` com o **ID do pedido**, **processar assíncrono** (RabbitMQ) e expor **status por HTTP**.

## Sumário
- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Configuração](#configuração)
- [Endpoints](#endpoints)
- [Mensageria (RabbitMQ)](#mensageria-rabbitmq)
- [Logs & Observabilidade](#logs--observabilidade)
- [Testes](#testes)
- [Problemas comuns](#problemas-comuns)
- [Estrutura do projeto](#estrutura-do-projeto)

---

## Arquitetura
- **REST**:
  - `POST /api/pedidos` → valida payload, **gera ID**, registra status **RECEBIDO**, publica mensagem na fila e retorna `202 Accepted` com `{ "id": "<uuid>" }`.
  - `GET /api/pedidos/status/{id}` → retorna o **status atual** (`DESCONHECIDO | RECEBIDO | PROCESSANDO | SUCESSO | FALHA`) e, em caso de falha, a mensagem de erro.
- **Processamento assíncrono**:
  - Consumidor lê da fila de **entrada**, simula trabalho (1–3s) e **20% de falha** (mensagem vai para **DLQ**).
  - Em caso de sucesso, publica também em uma fila de **status de sucesso**; em caso de falha, em **status de falha**.
  - Um **store em memória** (ex.: `ConcurrentHashMap`) mantém o status por `id` para o endpoint de consulta.

> Observação: o armazenamento em memória atende ao escopo do teste. Em produção, prefira persistência externa (Redis/DB).

---

## Pré-requisitos
- **Java 17+**
- **Maven 3.9+**
- **RabbitMQ** (local ou **CloudAMQP**)
  - Acesso aos hosts/credenciais da sua instância.

---

## Configuração
Defina as filas e as credenciais no `src/main/resources/application.yaml` (exemplo):

```yaml
server:
  port: 8080

spring:
  application:
    name: pedidos-backend
  rabbitmq:
    # Local
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    listener:
      simple:
        default-requeue-rejected: false  # necessário p/ DLQ

# Filas usadas pelo app
app:
  queues:
    entrada: pedidos.entrada.ricardo
    entradaDlq: pedidos.entrada.ricardo.dlq
    statusSucesso: pedidos.status.sucesso.ricardo
    statusFalha: pedidos.status.falha.ricardo

springdoc:
  swagger-ui:
    path: /swagger-ui.html
  api-docs:
    path: /v3/api-docs
```

> Usando **CloudAMQP**? Troque `spring.rabbitmq.*` para:
> ```yaml
> spring:
>   rabbitmq:
>     host: jaragua-01.lmq.cloudamqp.com
>     port: 5672
>     username: <user>
>     password: <pass>
>     virtual-host: <vhost>
> ```
> Se sua instância exigir TLS, use `addresses: amqps://user:pass@host/vhost`.

## Endpoints

### 1) Criar pedido
`POST /api/pedidos`  
**Request (JSON)**
```json
{
  "produto": "Teclado Mecânico",
  "quantidade": 1
}
```

**Responses**
- `202 Accepted`
  ```json
  { "id": "3b5a2b7e-7b0b-4c2a-a9b4-2d6a4a2d9b55" }
  ```
- `400 Bad Request`
  ```json
  { "code": "BAD_REQUEST", "message": "produto/quantidade inválidos" }
  ```

**cURL**
```bash
curl -i -X POST http://localhost:8080/api/pedidos   -H "Content-Type: application/json"   -d '{"produto":"Teclado Mecânico","quantidade":1}'
```

### 2) Consultar status
`GET /api/pedidos/status/{id}`

**Response 200**
```json
{
  "id": "3b5a2b7e-7b0b-4c2a-a9b4-2d6a4a2d9b55",
  "status": "PROCESSANDO",
  "mensagemErro": null,
  "dataProcessamento": "2025-09-29T15:20:19Z"
}
```

**Response 404**
```json
{ "code": "NOT_FOUND", "message": "Pedido não encontrado" }
```

**cURL**
```bash
curl -s http://localhost:8080/api/pedidos/status/3b5a2b7e-7b0b-4c2a-a9b4-2d6a4a2d9b55 | jq .
```

---

## Mensageria (RabbitMQ)

### Filas
- **Entrada**: `app.queues.entrada` (ex.: `pedidos.entrada.ricardo`)  
  Com DLQ configurada via argumentos:
  - `x-dead-letter-exchange`: `""` (default exchange)
  - `x-dead-letter-routing-key`: `app.queues.entradaDlq`
- **DLQ**: `app.queues.entradaDlq`
- **Status (sucesso)**: `app.queues.statusSucesso`
- **Status (falha)**: `app.queues.statusFalha`

### Comportamento do consumidor
- Ao consumir um pedido:
  - Marca `PROCESSANDO`, simula latência **1–3s**.
  - **20%** de chance de falha:
    - Publica mensagem em `statusFalha` com `mensagemErro`
    - Lança `AmqpRejectAndDontRequeueException` → mensagem original vai para **DLQ**
  - Caso contrário:
    - Publica mensagem em `statusSucesso`
    - Marca `SUCESSO`

> O **status exposto via HTTP** é sempre o do store em memória (atualizado pelo consumer).

---

## Logs & Observabilidade
Níveis úteis no `application.yaml`:
```yaml
logging:
  level:
    root: INFO
    org.springframework.amqp: INFO
    org.springframework.rabbit: INFO
    com.seu.pacote: DEBUG
```
No painel do **RabbitMQ/CloudAMQP**, você pode verificar **enfileiramento**, **consumo** e mensagens na **DLQ**.

---

## Testes
- **Unit tests**: validar que o publisher envia para a fila correta (ex.: verificar `RabbitTemplate.convertAndSend("", <fila>, <payload>)`).
- **Integração** (opcional): com broker real ou `Testcontainers` (se permitido).

---

## Problemas comuns
- **Status não muda para SUCESSO/FALHA**  
  Verifique se o **cliente usa o ID retornado** no `202` para fazer o polling. Se o front gerar outro ID, o status nunca vai casar.
- **DLQ vazia mesmo com falhas**  
  Confirme `default-requeue-rejected: false` e o `throw new AmqpRejectAndDontRequeueException(...)` no consumidor.
- **Conexão no RabbitMQ**  
  Confira host/usuário/senha/vhost. Em CloudAMQP, valide se o IP tem permissão de acesso e se o vhost está correto.
- **YAML inválido**  
  Indentação incorreta quebra o boot. Use espaços (não TAB).

