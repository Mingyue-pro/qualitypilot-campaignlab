# CampaignLab

Machine-readable API contract: [`openapi/openapi.json`](openapi/openapi.json) (OpenAPI 3.1.0). Human-readable index: [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md).

CampaignLab is the independently runnable Java system under test for QualityPilot. Its confirmed V1.0 direction is a synthetic marketing-flow platform inspired by the user's MDP domain experience; it is not a copy of a former employer's system.

## Architecture

- `frontend/`: Vue 3 + TypeScript + Vite, runs independently on port `5173`.
- `src/`: Spring Boot REST API, runs independently on port `8080`.
- MySQL 8.4: runtime business database, schema managed by Flyway.
- H2: test-profile database only; it is not the runtime database.
- `doris_user_profile`: a MySQL table that simulates the Doris profile source in V1.0.

## Run MySQL

Docker is not installed on the current development host, so the Compose definition is configured but has not yet been executed here.

```bash
docker compose up -d mysql
```

## Run backend

```bash
./mvnw spring-boot:run
```

The REST API is available at `http://localhost:8080/api`.

## Run frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` to the Spring Boot backend.

## Test

```bash
./mvnw test
cd frontend && npm run build
```

The Java suite currently uses the isolated `test` profile with H2 in MySQL compatibility mode. A real-MySQL integration run remains required once Docker or a local MySQL server is available; do not describe MySQL behavior as verified before that run passes.

The current runtime still contains the earlier campaign/reward technical slice.
The target V1.0 contract and synthetic knowledge pack are in `docs/` and
`knowledge/bugs/`; Java runtime migration is in progress. Do not present the
marketing-flow functions as implemented until their integration tests pass.
