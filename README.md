# Lost-Found

Campus lost-and-found system with an independent Vue user portal, a RuoYi
administration console, and a modular Spring Boot backend.

## Repository Layout

| Directory | Purpose |
| --- | --- |
| `portal/` | Campus user application: posts, claims, messages, reports and profiles |
| `admin/` | RuoYi administration frontend |
| `backend/` | Spring Boot backend; campus business logic is in `ruoyi-lostfound` |
| `backend/sql/` | Versioned schema, initialization, migration and verification SQL |
| `docs/` | Requirements, data model, deployment and acceptance documentation |
| `scripts/` | Local startup helpers and browser regression tests |
| `legacy-backend/` | Original backend source retained for migration comparison only |

The active implementation is `backend/`, `admin/` and `portal/`. Historical
workspace paths and task statuses in inherited portal and legacy documents are
not the current baseline. Start with `docs/REFACTOR_EXECUTION.md`.

## Prerequisites

- JDK 17 or newer; Maven is provided through `backend/mvnw` and `mvnw.cmd`.
- Node.js satisfying `^20.19.0 || >=22.12.0`, and npm.
- MySQL 8 and Redis for running the backend.

Install frontend dependencies with `npm ci` in both `portal/` and `admin/`.
Both `package-lock.json` files are versioned to keep dependency installation
reproducible.

## Configuration and Database

Read `docs/DEPLOYMENT.md`, `docs/DATA_MIGRATION_V1_TO_V2.md` and
`docs/ACCEPTANCE_RUNBOOK.md` before initializing a new database.
Use a dedicated development database and distinguish fresh installation SQL
from migration SQL. Do not run all SQL files indiscriminately.

`backend/.env.example` lists the backend environment variables. It is a
template, not an automatically loaded configuration file. Supply database
credentials and production secrets through the process environment or a
secret manager.

Git intentionally excludes database backups, uploaded files, runtime logs,
build output, `node_modules`, private keys and the original backend's local
`application.yaml`. These local files are not deleted. Keep a separate secure
backup for database contents and uploads; this repository is not a data backup.
The legacy backend is a source reference, not a configured runtime service.

## Build and Test

Run these commands from the respective directories in PowerShell:

```powershell
# backend/
.\mvnw.cmd -pl ruoyi-admin -am test
.\mvnw.cmd -pl ruoyi-admin -am package -DskipTests

# portal/
npm.cmd ci
npm.cmd test
npm.cmd run build

# admin/
npm.cmd ci
npm.cmd run build:prod
```

`.github/workflows/ci.yml` defines backend tests and packaging, portal tests and
build, and the administration build. Browser regression scripts are in
`scripts/tests/`; mocked interface tests do not replace live business acceptance.

## Run Locally

After building the backend and preparing MySQL, Redis and the schema:

```powershell
# From the repository root. Prompts for the MySQL password.
.\scripts\Start-LostFound.cmd
```

In separate terminals, run `npm.cmd run dev` from `portal/` and `admin/`.
Default local ports are `5173` for the portal, `80` for administration, and
`8081` for the backend. Check the actual startup output for port conflicts.

Do not use development credentials, public database ports or the local Vite
servers as a production deployment. Preserve upstream license files and notices
when distributing the project.
