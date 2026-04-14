# MerchantOps Admin Web

Minimal Vite + React admin console for the Productization Baseline.

This app is intentionally thin. Slice A proves the frontend placement, local run path, login flow, current tenant context, token restoration, and navigation placeholders before adding workflow screens.

## Stack

- Vite
- React
- TypeScript
- React Router
- TanStack Query
- Tailwind CSS
- Zod

## Local Run

Install dependencies:

```powershell
cd merchantops-admin-web
npm install
```

Run the dev server:

```powershell
npm run dev
```

The Vite app runs at `http://localhost:5173` by default. API calls use relative `/api/...` paths and the Vite dev proxy forwards them to `http://localhost:8080`.

## Backend Prerequisite

Start the backend from the repository root before logging in:

```powershell
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd -pl merchantops-api -am -DskipTests install
.\mvnw.cmd -f merchantops-api/pom.xml spring-boot:run
```

The Dockerized API path documented in `../docs/getting-started/quick-start.md` also works.

## Demo Login

Use the seeded admin account:

- Tenant: `demo-shop`
- Username: `admin`
- Password: `123456`

The dashboard calls:

- `POST /api/v1/auth/login`
- `GET /api/v1/context`

Roles and permissions displayed in the dashboard are decoded from JWT claims for operator visibility only. They are not used as an authorization source.

## Session Boundary

Slice A stores the JWT access token in `localStorage` under `merchantops.admin.auth.v1` with a client-side expiry timestamp derived from `expiresIn`.

On page refresh, the app restores the token, refetches `/api/v1/context`, and clears the session on expired, invalid, `401`, or `403` responses.

There is no backend refresh-token, logout, or token-revocation flow yet. The `Clear session` action only removes the local token. Authentication lifecycle completion is planned as the next Productization Baseline slice.

## Verification

```powershell
npm run typecheck
npm run lint
npm run build
```

Manual smoke:

1. Start the backend.
2. Start this frontend with `npm run dev`.
3. Open `http://localhost:5173`.
4. Log in with `demo-shop` / `admin` / `123456`.
5. Confirm the dashboard shows tenant, operator, token roles, and token permissions.
6. Refresh the page and confirm context reloads.
7. Use `Clear session` and confirm the app returns to login.

## Image Credit

The login-page operations image is loaded from Wikimedia Commons:

- [Warehouse distribution-center-1136510.jpg](https://commons.wikimedia.org/wiki/File:Warehouse_distribution-center-1136510.jpg), by Rsherwin, licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/deed.en)
