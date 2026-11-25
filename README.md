# Real-Time Order Processing System

I built an event-driven order processing application that accepts an order through a REST API and processes it asynchronously with Kafka. The caller gets an order ID immediately and can use it to check when processing finishes.

**Tech stack:** Java 21, Spring Boot, Apache Kafka, PostgreSQL, Redis, Docker Compose, JUnit, and Mockito.

## How an order moves through the system

1. **Create the order.** `POST /orders` accepts a product SKU and quantity. The API validates the request, saves the order in PostgreSQL, and returns its ID with the status `PENDING`.
2. **Publish the event.** A scheduled publisher picks up unpublished orders and sends their IDs to the `orders.created` Kafka topic.
3. **Process the order.** A Kafka consumer receives the ID and updates the order from `PENDING` to `PROCESSED`.
4. **Read the result.** `GET /orders/{id}` returns the current status. Once an order is processed, its status can be served from Redis.

## Design decisions

- **Recovering from a failed publish.** I stored a publication flag on each order and set it only after Kafka acknowledged the message. If a send fails, the publisher can pick up the order again.
- **Handling duplicate events.** I made the consumer update only rows whose status is still `PENDING`. If Kafka delivers the same order ID twice, the second delivery leaves the completed order alone.
- **Keeping status reads current.** I cached only `PROCESSED`, with a ten-minute expiration. This keeps an old `PENDING` value from hiding a completed order. If Redis is unavailable, the API reads from PostgreSQL.

## How I tested it

I ran the API, Kafka, PostgreSQL, and Redis locally with Docker Compose. I first submitted an order from PowerShell, then ran a Python demo that created a new order and polled the API until processing finished. Both responses below came from the same demo run.

| Step | Order ID | Status |
| --- | --- | --- |
| Order created | `60fa4b4d-6baa-4255-a67d-97b42ba00278` | `PENDING` |
| Status checked | `60fa4b4d-6baa-4255-a67d-97b42ba00278` | `PROCESSED` |

The project includes seven JUnit and Mockito tests covering API validation and responses, Kafka publication success and failure, Redis cache hits, and database fallback.

## Run locally

With Docker Compose and Python 3 installed, run these commands from the project folder:

```sh
docker compose up --build -d
python scripts/demo.py
```

The demo prints `Created` and `Processed` for the same order ID.

| Endpoint | Purpose |
| --- | --- |
| `POST /orders` | Create an order |
| `GET /orders/{id}` | Check its status |
| `GET /actuator/health` | Check application health |
| `GET /actuator/metrics/http.server.requests` | Inspect HTTP request metrics |

The local API runs at `http://127.0.0.1:18080`. To stop the services, run `docker compose down`.
