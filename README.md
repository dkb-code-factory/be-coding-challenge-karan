# Read Me First

All the information about the code challenge is in [CODE_CHALLENGE.md](./CODE_CHALLENGE.md)

# Getting Started

1. start docker-compose with postgres
2. start the app
3. Hit the following endpoints to test the service:

```bash
curl -X POST -H "Content-Type: application/json" localhost:8080/register -d '{ "id": "bcce103d-fc52-4a88-90d3-9578e9721b36", "notifications": ["type1","type5"]}'
curl -X POST -H "Content-Type: application/json" localhost:8080/notify -d '{ "userId": "bcce103d-fc52-4a88-90d3-9578e9721b36", "notificationType": "type5", "message": "your app rocks!"}'
```


