# template-app-name

A fullstack application by **Finaxys** with an Angular 21 frontend and a Spring Boot 4 (Java 25) backend.

---

## Stack

| Layer    | Technology              |
|----------|-------------------------|
| Frontend | Angular 21, Bootstrap 5 |
| Backend  | Spring Boot 4, Java 25  |
| Database | PostgreSQL 16           |
| Build    | Maven, Node 22 / npm 10 |
| Docker   | Docker Compose          |

---

## Prerequisites

- [Java 25](https://adoptium.net/)
- [Node.js 22+](https://nodejs.org/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (running)

---

## How to run

### Option 1 — Docker Compose *(recommended)*

Builds everything and starts Postgres + the app in one command.

```bash
docker compose up --build
```

Open **http://localhost:8080**.

> First build downloads Node, npm packages and Maven dependencies — takes a few minutes.  
> Subsequent starts (no code changes): `docker compose up`

To stop: `docker compose down`

---

### Option 2 — Local development with hot reload

**Terminal 1 — Backend** (requires Postgres running):
```bash
docker-compose up postgres -d
./mvnw spring-boot:run
```

**Terminal 2 — Frontend**:
```bash
cd frontend
npm install      # first time only
npm start
```

| | URL |
|---|---|
| Frontend (hot reload) | http://localhost:4200 |
| Backend API | http://localhost:8080 |

> The Angular dev server proxies all `/api/*` requests to port 8080 automatically.

---

### Option 3 — Production JAR

Builds Angular and bundles it into the Spring Boot JAR.

```bash
./mvnw clean package
java -jar target/template-app-name-0.0.1-SNAPSHOT.jar
```

Open **http://localhost:8080**.

---

## Testing

### Backend tests

Run the Spring Boot tests with Maven:

```bash
./mvnw test
```

This runs unit and integration tests, including those using Testcontainers for PostgreSQL.

### Frontend tests

Run the Angular tests:

```bash
cd frontend
npm test
```

This runs unit tests using Vitest.

---

## Pages & API

| Page | Route | Backend endpoint |
|---|---|---|
| Hello World | `/hello` | `GET /api/greetings` |
| Good Night World | `/good-night` | `GET /api/greetings/key/good-night` |

Available greeting API endpoints:

- `GET /api/greetings`
- `GET /api/greetings/{id}`
- `GET /api/greetings/key/{key}`
- `POST /api/greetings` → `201 Created`
- `PUT /api/greetings/{id}`
- `DELETE /api/greetings/{id}` → `204 No Content`

All `/api/*` errors follow the RFC problem details format (`application/problem+json`):

```json
{
  "type": "urn:template-app:problem:greeting-not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Greeting not found",
  "instance": "/api/greetings/42"
}
```

Validation errors add an `errors` extension property:

```json
{
  "type": "urn:template-app:problem:validation-error",
  "title": "Unprocessable Content",
  "status": 422,
  "detail": "Request validation failed",
  "instance": "/api/greetings",
  "errors": {
    "key": "must not be blank"
  }
}
```

Typical problem types include `urn:template-app:problem:unauthorized`, `urn:template-app:problem:forbidden`, `urn:template-app:problem:validation-error`, `urn:template-app:problem:invalid-request-body`, `urn:template-app:problem:greeting-not-found`, and `urn:template-app:problem:greeting-key-conflict`.

---

## Permission management

Access control is enforced on two layers: the **Angular frontend** (UX gating) and the **Spring Boot backend** (hard enforcement).

### How it works

Permissions are profile-based. Each user is assigned one or more profiles in Keycloak (as client roles). On login, the frontend fetches the user's effective permissions from `GET /api/me` and stores them in a client-side signal. The backend independently validates every request using `@PreAuthorize`.

A permission is defined by three attributes:
- **resource** — what the permission applies to (e.g. `GREETINGS`)
- **action** — what operation is allowed (`READ`, `CREATE`, `UPDATE`, `DELETE`, `PUBLISH`)
- **scope** — the data boundary (`ALL`, `ORG`, `TEAM`, `SELF`)

### Profile examples

| Profile key | Allowed actions on `GREETINGS` |
|---|---|
| `collaborator` | `CREATE` |
| `hr` | `READ`, `CREATE`, `UPDATE` |
| `admin` | `READ`, `CREATE`, `UPDATE`, `DELETE`, `PUBLISH` |

### Add a permission to an existing role

Edit **`src/main/resources/security/permission-profiles.json`** — add the action to the relevant profile's `actions` array.

### Create a new role

1. **`src/main/resources/security/permission-profiles.json`** — add a new profile object with a `key`, `label`, and `permissions` array.
2. **Keycloak admin console** — create a client role with the same `key` on the `template-app-spring-api` client.

If the new role requires an action that does not exist yet, also update:
- **`src/main/java/.../security/permission/Action.java`** — add the value to the enum.
- **`frontend/src/app/auth/auth.types.ts`** — add the same value to the `PermissionAction` union type.

### Protect a page

In **`frontend/src/app/app.routes.ts`**, add `canActivate: [authGuard]` and a `data.permission` object to the route:

```ts
{
  path: 'my-page',
  canActivate: [authGuard],
  data: {
    permission: { resource: 'MY_RESOURCE', action: 'READ', scope: 'ALL' },
  },
  loadComponent: () => import('./pages/my-page/my-page').then((m) => m.MyPage),
},
```

The guard redirects to `/forbidden` automatically if the permission is missing.

### Protect the spring boot backend

Always enforce permissions on backend endpoints. Frontend checks are for UX only.

1. Add `@PreAuthorize` on every protected endpoint.
2. Use the same permission contract: `resource`, `action`, `scope`.

```java
@GetMapping("/greetings/{id}")
@PreAuthorize("@permissions.has(authentication, 'GREETINGS', 'READ', 'ALL')")
public GreetingView getById(@PathVariable Long id) { ... }
```

When a new backend action is introduced, update:
- `src/main/java/.../security/permission/Action.java`
- `src/main/resources/security/permission-profiles.json`

### Hide or disable a button / component

In the component's `.ts` file, inject `PermissionStoreService` and declare a `computed`:

```ts
private readonly permissionStore = inject(PermissionStoreService);

readonly canEdit = computed(() =>
  this.permissionStore.hasPermission({ resource: 'MY_RESOURCE', action: 'UPDATE', scope: 'ALL' })
);
```

Then in the template:

```html
<!-- Hide entirely -->
@if (canEdit()) {
  <button>Edit</button>
}

<!-- Or disable without hiding -->
<button [disabled]="!canEdit()">Edit</button>
```

---

## Project structure

```
template-app-name/
├── frontend/                        # Angular 21 app
│   ├── src/app/
│   │   ├── pages/
│   │   │   ├── hello-world/
│   │   │   └── good-night-world/
│   │   ├── app.routes.ts
│   │   └── app.config.ts
│   └── proxy.conf.json              # Dev proxy: /api → :8080
├── src/main/
│   ├── java/com/finaxys/templateappname/
│   │   ├── controller/
│   │   │   ├── GreetingController.java
│   │   │   └── SpaController.java
│   │   └── config/
│   │       └── WebConfig.java
│   └── resources/
│       ├── application.properties
│       └── static/                  # Angular build output (auto-generated)
├── docker-compose-db-only.yaml      # Only Postgres service
├── docker-compose.yml               # Full Docker deployment
├── Dockerfile
└── pom.xml
```
