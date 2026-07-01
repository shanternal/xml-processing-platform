# XML Processing Platform

The platform consists of three services:

* **request-service** — accepts XML requests, sends them to the conversion service, stores the processed result in PostgreSQL (deduplicated by XML hash), and provides an API for retrieving processed requests. A scheduled job periodically migrates older results to storage-service (S3).
* **xml2json-service** — converts XML to JSON.
* **storage-service** — stores and retrieves raw objects in an S3-compatible object storage (MinIO).

## Technologies

* Java 21
* Spring Boot
* Hibernate
* PostgreSQL
* Liquibase
* Maven
* Docker Compose
* MinIO

## Project Structure

```text
xml-processing-platform/
├── request-service/
├── xml2json-service/
├── storage-service/
├── docker-compose.yaml
├── .env.example
└── postman-collections/
```

## Running the Application

### Prerequisites

* Docker
* Docker Compose

### Quick Start

```bash
cp .env.example .env && docker compose up -d --build
```

### Manual Setup

1. Copy the environment file:

```bash
cp .env.example .env
```

2. Build the project:

```bash
docker compose build
```

3. Start the services:

```bash
docker compose up -d
```

Once the application is running, the following services will be available:

| Service          | URL                    |
|------------------|------------------------|
| request-service  | http://localhost:8080  |
| xml2json-service | http://localhost:8081  |
| storage-service  | http://localhost:8082  |
| PostgreSQL       | http://localhost:5432  |
| MinIO API        | http://localhost:9000  |
| MinIO Console    | http://localhost:9001  |

### Database Migrations

Database migrations are executed automatically when `request-service` starts using Liquibase.

### Checking Service Status

```bash
docker compose ps
docker compose logs -f
docker compose logs request-service
docker compose logs xml2json-service
docker compose logs storage-service
docker compose logs minio
```

### Stopping the Services

Stop the services:

```bash
docker compose down
```

Stop the services and remove the database volume:

```bash
docker compose down -v
```

## API Endpoints

### request-service

**Configuration**

| Environment variable        | Default | Description                                              |
|------------------------------|---------|------------------------------------------------------------|
| `MIGRATION_ENABLED`          | `true`  | Enables the scheduled migration of results to storage-service. |
| `MIGRATION_INTERVAL_MS`      | `60000` | Delay between migration batch runs.                        |
| `MIGRATION_INITIAL_DELAY_MS` | `30000` | Delay before the first migration run after startup.        |
| `MIGRATION_BATCH_SIZE`       | `20`    | Max records migrated per run.                               |

#### POST `/api/v1/request`

Converts XML to JSON, stores the result in the database, and returns the created record.

**Request**

```http
POST /api/v1/request
Content-Type: application/xml
```

```xml
<product id="101" category="electronics">
  <name>Laptop Pro</name>
  <price currency="USD">1299.00</price>
  <inStock>true</inStock>
</product>
```

**Response**

```json
{
  "id": 10,
  "json": {
    "product": {
      "price": {
        "currency": "USD",
        "content": 1299
      },
      "name": "Laptop Pro",
      "inStock": true,
      "id": 101,
      "category": "electronics"
    }
  }
}
```

---

#### GET `/api/v1/request/{id}`

Retrieves information about a processed request.

**Request**

```http
GET /api/v1/request/3
```

Once a record is migrated to storage-service, its data is transparently fetched from S3 instead of PostgreSQL; the response shape is unchanged.

**Response**

```json
{
  "id": 3,
  "canonicalXml": "<product category=\"electronics\" id=\"101\">\n  <name>Laptop Pro</name>\n  <price currency=\"USD\">1299.00</price>\n  <inStock>true</inStock>\n</product>",
  "json": {
    "product": {
      "price": {
        "currency": "USD",
        "content": 1299
      },
      "name": "Laptop Pro",
      "inStock": true,
      "id": 101,
      "category": "electronics"
    }
  },
  "requestedAt": "2026-06-29T15:18:51.672731Z",
  "processingTimeMs": 8,
  "xmlTagsCount": 4,
  "jsonKeysCount": 8
}
```

---

#### GET `/api/v1/page`

Returns a paginated list of processed requests with filtering support.

**Request**

```http
GET /api/v1/page?page=0&size=3&processingTimeMsMin=0&processingTimeMsMax=500&xmlTagsCountMin=3&xmlTagsCountMax=10&jsonKeysCountMin=2&jsonKeysCountMax=19&requestedAtFrom=2026-02-01T00:00:00Z&requestedAtTo=2026-12-31T23:59:59Z
```

**Response**

```json
{
  "content": [
    {
      "id": 9,
      "requestedAt": "2026-06-29T16:14:32.379472Z",
      "processingTimeMs": 19,
      "xmlTagsCount": 4,
      "jsonKeysCount": 8
    },
    {
      "id": 8,
      "requestedAt": "2026-06-29T16:10:51.756562Z",
      "processingTimeMs": 1,
      "xmlTagsCount": 4,
      "jsonKeysCount": 4
    },
    {
      "id": 7,
      "requestedAt": "2026-06-29T16:10:51.741881Z",
      "processingTimeMs": 1,
      "xmlTagsCount": 4,
      "jsonKeysCount": 8
    }
  ],
  "page": 0,
  "size": 3,
  "totalElements": 7,
  "totalPages": 3
}
```

Supported filters:

* `processingTimeMsMin`
* `processingTimeMsMax`
* `xmlTagsCountMin`
* `xmlTagsCountMax`
* `jsonKeysCountMin`
* `jsonKeysCountMax`
* `requestedAtFrom`
* `requestedAtTo`

### xml2json-service

#### POST `/api/v1/xml2json`

Converts XML to JSON.

**Request**

```http
POST /api/v1/xml2json
Content-Type: application/xml
```

```xml
<product id="101" category="electronics">
    <name>Laptop Pro</name>
    <price currency="USD">1299.00</price>
    <inStock>true</inStock>
</product>
```

**Response**

```json
{
  "product": {
    "price": {
      "currency": "USD",
      "content": 1299
    },
    "name": "Laptop Pro",
    "inStock": true,
    "id": 101,
    "category": "electronics"
  }
}
```

### storage-service

Sends and retrieves raw objects to/from an S3-compatible object storage (MinIO).

**Configuration**

| Environment variable        | Default | Description                                                                 |
|------------------------------|---------|-------------------------------------------------------------------------------|
| `STORAGE_MAX_UPLOAD_SIZE`    | `100MB` | Maximum size of a request body the service will buffer in memory before uploading to S3. |

#### POST `/api/v1/objects`

Uploads an object to the storage. Accepts a request body of any content type (`consumes = MediaType.ALL_VALUE`). The body is buffered in memory and then uploaded to S3, so a `Content-Length` header is not required — chunked transfer encoding works as well.

**Request**

```http
POST /api/v1/objects
Content-Type: text/plain
```

```text
Hello, storage service! This is a plain text object.
```

**Response**

```http
201 Created
Location: http://localhost:8082/api/v1/objects/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

```json
{
  "storageKey": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sizeInBytes": 54
}
```

If the request body is empty, the service returns `400 Bad Request`. If the buffered body exceeds `STORAGE_MAX_UPLOAD_SIZE`, it returns `413 Payload Too Large`. If the `Content-Type` header is not a valid MIME type, it returns `415 Unsupported Media Type`.

---

#### GET `/api/v1/objects/{key}`

Downloads an object by its storage key. The object content is streamed back to the client with `Content-Type` and `Content-Length` taken from the S3 object metadata.

**Request**

```http
GET /api/v1/objects/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

**Response**

```http
200 OK
Content-Type: text/plain
Content-Length: 54
```

```text
Hello, storage service! This is a plain text object.
```

If the key does not exist in the storage, the service returns `404 Not Found`.

---

#### HEAD `/api/v1/objects/{key}`

Returns object metadata (`Content-Type`, `Content-Length`) without downloading its content.

**Request**

```http
HEAD /api/v1/objects/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

**Response**

```http
200 OK
Content-Type: text/plain
Content-Length: 54
```

If the key does not exist in the storage, the service returns `404 Not Found`.

---

#### DELETE `/api/v1/objects/{key}`

Deletes an object by its storage key. The operation is idempotent — deleting a non-existent key still returns `204 No Content`.

**Request**

```http
DELETE /api/v1/objects/3fa85f64-5717-4562-b3fc-2c963f66afa6
```

**Response**

```http
204 No Content
```

## Postman

The Postman collections are located in the following directory:

```text
postman-collections/
├── request-service.postman_collection.json
├── xml2json-service.postman_collection.json
└── storage-service.postman_collection.json
```