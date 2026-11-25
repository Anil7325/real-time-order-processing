# Real-Time Order Processing System

Submit a product SKU and quantity over HTTP, then look up the order while a Kafka consumer processes it. This Java 21 portfolio project runs the API and the worker in one Spring Boot application. Processing simulates order acceptance by recording a status change.

## Run locally

Start Docker Desktop (or Docker Engine with Compose). Open a terminal in the folder containing `compose.yaml` and run:

```sh
docker compose up --build -d
```

The first build downloads the images and Java dependencies and runs the JUnit and Mockito tests. Open http://localhost:18080/actuator/health to see when the app is ready.

### Try it from Windows PowerShell

```powershell
$order = Invoke-RestMethod -Method Post -Uri http://localhost:18080/orders -ContentType 'application/json' -Body '{"sku":"BOOK-1","quantity":2}'
$order
Invoke-RestMethod "http://localhost:18080/orders/$($order.id)"
```

The POST returns a UUID and `PENDING`. Run the last line again if the worker is still processing. The final status is `PROCESSED`.

For an automated demo, run `python scripts/demo.py` (or `python3 scripts/demo.py`). It waits for startup, submits one order, polls the status, and exits successfully once that order is processed.

## How an order moves through the system

1. **Accept.** The API validates the input and inserts one PostgreSQL row with status `PENDING` and `published=false`. It returns HTTP 201 and the status URL.
2. **Publish.** Once per second, a scheduled producer reads unpublished rows and sends each order ID to the `orders.created` Kafka topic. It marks the row published after broker acknowledgment. A failed send leaves the row available for retry.
3. **Process.** The consumer changes the status with a conditional SQL update. If the producer crashes between Kafka acknowledgment and the database update, it may send the event twice. The second delivery changes zero rows. This keeps status updates idempotent.
4. **Read.** A status lookup checks Redis and falls back to PostgreSQL on a miss or Redis connection error. Only the terminal status is cached, for ten minutes, so polling cannot get stuck on an old pending value.

## API

| Request | Result |
| --- | --- |
| POST /orders | 201 with ID, initial status and Location header |
| GET /orders/{id} | 200 with ID and current status |
| GET /actuator/health | Service health |
| GET /actuator/metrics/http.server.requests | HTTP timing and request counts after the API has received traffic |

A blank SKU, a SKU longer than 80 characters, or a nonpositive quantity returns 400. An unknown UUID returns 404. Each successful POST creates a distinct order. The demo uses one Kafka broker and an internal topic carrying order IDs.

## Tests and logs

Run `mvn test` with Maven and JDK 21 to execute unit tests without starting the services. GitHub Actions builds the Compose stack and runs the end-to-end demo on pushes and pull requests.

Use `docker compose logs -f app` to watch processed order IDs or inspect failures. Stop with `docker compose down`. PostgreSQL and Kafka use named volumes so orders and events survive a normal restart.

## Publish to GitHub

Create an empty GitHub repository named `real-time-order-processing`. From this project folder, run the following, replacing YOUR-USERNAME with your GitHub username:

```sh
git init -b main
git add .
git commit -m "Build event-driven order processing demo"
git remote add origin https://github.com/YOUR-USERNAME/real-time-order-processing.git
git push -u origin main
```
