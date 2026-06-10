# XML Processing Platform

Проект предназначен для конвертации XML в JSON.


## Используемые технологии

- Java 17+
- Spring Boot
- Hibernate
- PostgreSQL
- Maven
- Docker Compose


## Структура проекта

```text
xml-processing-platform/
└── xml2json-service/
```

## Запуск

```bash
cd xml2json-service
docker build -t xml2json-service .
docker run -p 8081:8081 xml2json-service
```

### Ручки

| Сервис | Метод | URL             | Описание |
|--------|-------|-----------------|----------|
| xml2json-service | POST | api/v1/xml2json | Конвертация XML в JSON |