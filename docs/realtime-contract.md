# Realtime Contract (Phase 1)

## Goal
Use HTTP for order command submission and WebSocket/STOMP for push updates.

## Command path (unchanged)
- `POST /api/order/new`
- Returns created order response.
- Backend asynchronously validates/executes via Kafka pipeline.

## WebSocket connection
- Endpoint: `ws://<host>:<port>/ws`
- STOMP app prefix: `/app`
- STOMP topic prefix: `/topic`

## Order status events
### Subscriptions
- User scoped: `/topic/users/{clientId}/orders`
- Order scoped: `/topic/orders/{orderId}`

### Payload
`OrderStatusEvent`
- `orderId: UUID`
- `clientId: UUID`
- `portfolioId: UUID`
- `status: PENDING | ACCEPTED | REJECTED | FULFILLED | CANCELLED`
- `message: string`
- `statusDate: Instant`
- `createdDate: Instant`

## Live quote updates
### Subscribe command
Client sends STOMP message:
- destination: `/app/quotes/subscribe`
- payload: `{ "symbol": "AAPL", "type": "EQUITY" }`

### Quote topic
- `/topic/quotes/{SYMBOL}/{TYPE}`
- Example: `/topic/quotes/AAPL/EQUITY`

### Payload
`QuoteUpdateEvent`
- `requestedSymbol: string`
- `instrumentType: EQUITY | CASH | CRYPTO`
- `marketSymbol: string`
- `price: number`
- `bid: number`
- `ask: number`
- `change: number`
- `changePercent: number`
- `previousClose: number`
- `currency: string`
- `asOf: Instant`
- `marketState: string`

## Notes
- Quote events are published on-demand for tracked symbols and refreshed every ~5s.
- REST quote endpoints remain available as fallback.
- This phase does not change order placement semantics.
