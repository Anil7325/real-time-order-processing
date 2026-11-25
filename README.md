Real-Time Order Processing System
I built a Java 21 and Spring Boot application to explore a common order-processing problem: how can an API confirm that it received an order while a background worker is still processing it? In this project, the API saves the order in PostgreSQL and immediately returns an order ID. A Kafka consumer then updates its status from `PENDING` to `PROCESSED`.
I kept the API and worker in one Spring Boot application so I could run the whole flow locally and focus on two things: recovering from a failed publish and handling the same event twice.
What happens to an order
When I submit a SKU and quantity to `POST /orders`, the API validates both fields and inserts a pending order in PostgreSQL. The response includes a `Location` header pointing to `GET /orders/{id}`, where I can follow that order.
A scheduled publisher checks PostgreSQL for unpublished orders once per second. It sends each order ID to the `orders.created` Kafka topic. The consumer uses that ID to update the corresponding database row. In this demo, processing an order means recording that status change.
Design decisions
Recovering from a failed publish. I store the publication flag on the order row itself. The publisher sets it only after Kafka acknowledges the message. If the broker is unavailable, the row stays eligible for the next attempt.
Handling duplicate events. A crash after Kafka acknowledges a message can cause the publisher to send it again. I made the consumer update conditional on `status = 'PENDING'`. After the first delivery changes the status, a second delivery updates zero rows.
Keeping status reads current. I cache only `PROCESSED` in Redis, for ten minutes. Caching `PENDING` could leave someone looking at an old status after the worker has finished. On a cache miss or a Redis connection error, the app reads from PostgreSQL.
How I verified it
I ran the four containers with Docker Compose, created an order from PowerShell, and checked its status by ID. I then ran the Python demo, which created a second order and polled the API until it finished. These are the two responses from that run:
```text
Created:   60fa4b4d-6baa-4255-a67d-97b42ba00278  PENDING
Processed: 60fa4b4d-6baa-4255-a67d-97b42ba00278  PROCESSED
```
The seven JUnit and Mockito tests cover order creation, invalid quantity, an unknown order ID, successful and failed Kafka publication, a Redis cache hit, and a database lookup when Redis is unavailable. The GitHub Actions workflow builds the stack and runs the end-to-end demo on pushes and pull requests.
Run locally
With Docker Compose and Python 3 installed, run these commands from the repository root:
```sh
docker compose up --build -d
python scripts/demo.py
```
The API listens at `http://127.0.0.1:18080`. Spring Boot Actuator exposes service health at `/actuator/health` and HTTP request metrics at `/actuator/metrics/http.server.requests`. Use `docker compose logs -f app` to follow processed order IDs and `docker compose down` to stop the stack.
