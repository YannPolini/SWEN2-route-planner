# SWEN2 Route Planner

Angular + Spring Boot route planner with PostgreSQL, OpenRouteService routing, weather forecasts, authentication, tour logs, and CSV import/export.

## Requirements

- Java 21 or newer
- Node.js 20 or newer
- npm
- Docker Desktop, or another local PostgreSQL 16 setup
- OpenRouteService API key
- Optional: OpenWeather API key for weather forecasts

You do not need to install Maven separately. The backend wrapper downloads Maven 3.9.9 on first use.

## First-Time Setup

### 1. Configure Backend Environment

Copy the backend environment template:

```powershell
Copy-Item backend\.env.example backend\.env
```

Fill in `backend/.env`:

```properties
DB_URL=jdbc:postgresql://localhost:5432/tourlogdb
DB_USERNAME=tourlog_user
DB_PASSWORD=tourlog_password
ORS_API_KEY=your-openrouteservice-key
OPENWEATHER_API_KEY=your-openweather-key
```

`backend/.env` is ignored by git and should not be committed.

### 2. Start PostgreSQL

```powershell
cd backend
docker compose up -d
```

The default database settings match `backend/.env.example`.

### 3. Start Backend

From `backend/`:

```powershell
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```sh
./mvnw spring-boot:run
```

The backend runs on `http://localhost:8080`.

### 4. Start Frontend

In a second terminal:

```powershell
cd frontend
npm install
npm start
```

The frontend runs on `http://localhost:4200`.

## Useful Commands

Backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

Frontend type check:

```powershell
cd frontend
.\node_modules\.bin\tsc.cmd -p tsconfig.app.json --noEmit
```

Frontend production build:

```powershell
cd frontend
npm run build
```

Stop the database:

```powershell
cd backend
docker compose down
```

Reset local database data:

```powershell
cd backend
docker compose down -v
docker compose up -d
```

## Project Layout

```text
backend/
  src/main/java/at/fhtechnikum/tourplanner/
    controller/    REST endpoints
    service/       business logic, routing, weather, import/export
    repository/    Spring Data JPA repositories
    model/         JPA entities
    dto/           request/response DTOs
  src/main/resources/
    application.example.properties
    data.sql
  compose.yaml
  .env.example
  mvnw, mvnw.cmd

frontend/
  src/app/
    auth/
    tours/
    tourlogs/
    import-export/
    shared/
  public/
    brand/
    metric-icons/
    transport-icons/
```

## Notes

- Secrets live in `backend/.env`, never in git.
- Generated folders such as `node_modules`, `.angular`, `dist`, `target`, Maven wrapper downloads, and logs are ignored.
- CSV is the currently supported import/export format.
- Demo data can be added from the app when the route library is empty.
