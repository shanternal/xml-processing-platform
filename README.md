# XML Processing Platform

The platform consists of two services:

* **request-service** — accepts XML requests, sends them to the conversion service, stores the processed result in PostgreSQL, and provides an API for retrieving processed requests.
* **xml2json-service** — converts XML to JSON.

## Technologies

* Java 21
* Spring Boot
* Hibernate
* PostgreSQL
* Liquibase
* Maven
* Docker Compose

## Project Structure

```text
xml-processing-platform/
├── request-service/
├── xml2json-service/
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

| Service          | URL                   |
|------------------| --------------------- |
| request-service  | http://localhost:8080 |
| xml2json-service | http://localhost:8081 |
| PostgreSQL       | http://localhost:5432 |

### Database Migrations

Database migrations are executed automatically when `request-service` starts using Liquibase.

### Checking Service Status

```bash
docker compose ps
docker compose logs -f
docker compose logs request-service
docker compose logs xml2json-service
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

## Postman

The Postman collections are located in the following directory:

```text
postman-collections/
├── request-service.postman_collection.json
└── xml2json-service.postman_collection.json
```
