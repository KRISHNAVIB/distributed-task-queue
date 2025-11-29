# Distributed Task Queue & Worker System (Prototype)

This project is a lightweight, production-inspired prototype of a distributed
job queue and worker system. It was built to demonstrate key
distributed-systems concepts such as reliable job processing, retries,
leases, rate limiting, concurrency control, and basic observability.

The system allows clients to submit jobs via REST, processes them through
multi-threaded workers, and exposes a small dashboard UI to inspect job
states in real time.

---

## Overview

The goal of the project is to demonstrate how a basic distributed job
processing system works — with concepts like job queues, workers, retries,
leases, and concurrency limits — implemented in a clean and simplified form
suitable for a prototype.

This implementation focuses on correctness, clarity, and showing essential
distributed-system building blocks within the assignment's time constraint.

### Key goals
- Submit jobs through a clean API  
- Persist and track state transitions  
- Process jobs with lease + retry logic  
- Apply rate limits & concurrency limits per tenant  
- Provide a minimal dashboard for visibility  
- Keep everything lightweight and easy to understand  

---

## Features Implemented

### ✔ Job Lifecycle
- `POST /jobs` to submit jobs  
- States: **pending → running → completed / failed**  
- Automatic retry of failed jobs  
- Dead Letter Queue (DLQ) for exhausted retries  

### ✔ Reliability
- Redis-backed queues for pending, running, and DLQ  
- TTL-based leases ensure stuck jobs are recovered  
- Idempotency keys prevent duplicate submissions  

### ✔ Rate Limiting
- Tenants limited to **10 job submissions per minute**  
- Implemented using Redis atomic counters  

### ✔ Concurrency Limits
- Maximum of **5 running jobs per tenant**  
- Enforced using Redis sets  

### ✔ Worker Model (ThreadPool Workers)
Workers are implemented using Spring’s `ThreadPoolTaskScheduler`:

```
8 worker threads
each thread independently polls Redis
```

While not separate JVM services, each thread behaves like a separate worker
instance, providing realistic concurrency for this prototype.

### ✔ Dashboard UI (HTML + JS)
- Displays pending, running, completed, and DLQ jobs  
- Form to submit new jobs  
- Polling-based live updates  
- No frameworks used (fast + easy to run)  

### ✔ Metrics
`GET /jobs/metrics` returns four counters:

- **submitted** → total jobs ever submitted  
- **completed** → jobs successfully processed  
- **failed** → number of failed processing attempts  
- **retried** → number of times jobs were re-queued  

These provide a basic view of throughput and reliability.

---

## API Endpoints

### Submit a Job
```
POST /jobs
```

Headers:
```
Idempotency-Key: <optional>
```

Body:
```json
{
  "tenantId": "t1",
  "payload": "work"
}
```

### Get Job Status
```
GET /jobs/{jobId}
```

### Job Lists
```
GET /jobs/pending
GET /jobs/running
GET /jobs/completed
GET /jobs/dlq
```

### Metrics
```
GET /jobs/metrics
```

---

## Worker Execution Model

The worker system uses a scheduled thread pool:

- 8 worker threads  
- Each thread polls Redis for pending jobs  
- When a job is picked:
  - It is “leased” (moved to running)  
  - Lease TTL prevents stuck jobs  
- On completion → moved to completed list  
- On failure → retried  
- If retries exhausted → moved to DLQ  

This model keeps the implementation simple but still demonstrates:
- safe concurrent job consumption  
- retry behavior  
- lease expiration handling  
- coordination via Redis atomic operations  

---

## Design Decisions & Trade-Offs

### 1. Thread-Based Workers Instead of Separate Services
**Reason:** Faster to implement and easy to review  
**Trade-Off:** Not a true distributed cluster; threads share memory

### 2. Redis as the Only Backend
**Reason:** Ideal for queues, counters, sets, and TTL  
**Trade-Off:** Not as durable as SQL/Kafka

### 3. Simple HTML/JS Dashboard
**Reason:** Quick to build, works without a build pipeline  
**Trade-Off:** Uses polling instead of WebSockets

### 4. Tenant in Request Body Instead of Authentication
**Reason:** Avoids implementing auth in a short assignment  
**Trade-Off:** Not secure for production

### 5. Single Repository for All Components
**Reason:** Makes the system easy to run and evaluate  
**Trade-Off:** Not a microservice architecture

---

## Project Structure

```
distributed-task-queue/
 ├── src/main/java/com/example/dtq/
 │     ├── controller/
 │     ├── service/
 │     ├── repository/
 │     ├── worker/
 │     ├── config/
 ├── frontend/
 │     ├── index.html
 │     └── main.js
 ├── README.md
```

---

## Running Locally

### 1. Start Redis
```
docker run -p 6379:6379 redis
```

### 2. Run Backend
Open project in IntelliJ and run the Spring Boot application.

### 3. Run UI
Open `frontend/index.html` using the “Live Server” extension in VS Code.

---

## Author

Built by **Krishna Shankar**  
Designed, implemented, and tested within the 10-hour timeline.
