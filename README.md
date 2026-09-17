# Photo Sharing Platform — TrizenAI Full Stack Internship Challenge

A full-stack photo-sharing app where an Admin/Lead creates events, a team collaboratively
uploads photos, the Admin selects and publishes a gallery, and a customer views it via a
shareable link + PIN — no customer account required.

**Stack:** HTML/CSS/vanilla JavaScript (frontend) + Java 17 / Spring Boot 3 (backend) +
H2 (dev) / PostgreSQL (prod) + local filesystem (dev) / AWS S3 (prod) for photo storage.

---

## 1. Project Overview

| Role | Capabilities |
|---|---|
| **Admin / Lead** | Register/login, create events, add team members, view all uploaded photos, select photos, publish gallery (generates link + PIN) |
| **Team Member** | Login, view assigned events, upload photos, view their own uploads. Cannot publish galleries or manage others' photos. |
| **Customer** | No account. Opens the gallery link, enters the PIN, browses published photos only. |

### Workflow
1. Admin creates an **Event** and adds **Team Members** (by their registered email).
2. Team Members **upload photos** to the event.
3. Admin **reviews all photos** and **selects** which ones go into the gallery.
4. Admin **publishes** the gallery → gets a shareable **link + PIN**.
5. Customer opens the link, enters the PIN, and views the selected photos.

---

## 2. Technology Stack

- **Frontend:** Plain HTML5, CSS3, vanilla JavaScript (no build step, no framework) — talks to
  the backend purely over `fetch()` REST calls.
- **Backend:** Java 17, Spring Boot 3.3 (Web, Security, Data JPA, Validation)
- **Auth:** JWT (stateless, `jjwt` library), BCrypt password hashing, Spring Security
  role-based authorization (`ROLE_ADMIN`, `ROLE_TEAM_MEMBER`)
- **Database:** H2 in-memory (zero-setup dev/eval default) → PostgreSQL in production
  (`prod` Spring profile)
- **Object Storage:** Pluggable `StorageService` interface —
  - `LocalStorageService` (default): saves files under `./uploads` on disk, served at
    `/uploads/**`. Zero cloud setup needed to run and evaluate the app.
  - `S3StorageService` (production): uploads to AWS S3. Enabled via
    `app.storage.provider=s3`. **No image bytes are ever stored in the database** — only
    metadata (photo id, event id, uploader, filename, storage key, size, timestamp),
    satisfying the "do not store image files in the DB" requirement in both modes.
- **PIN Security:** Gallery PINs are BCrypt-hashed at rest, exactly like passwords. The
  plaintext PIN is only ever returned once, in the publish response, for the Admin to copy
  and share.

---

## 3. System Architecture

```
┌────────────────────┐        REST/JSON over HTTPS        ┌──────────────────────────┐
│   Frontend (HTML/   │ ───────────────────────────────▶  │   Spring Boot Backend    │
│   CSS/JS) served     │ ◀───────────────────────────────  │   (Controllers → Services │
│   as static files    │        JWT in Authorization        │    → Repositories)        │
└────────────────────┘        header for protected routes  └───────────┬──────────────┘
                                                                        │
                                        ┌───────────────────────────────┼───────────────────────────┐
                                        ▼                               ▼                           ▼
                               ┌────────────────┐            ┌──────────────────┐        ┌──────────────────┐
                               │   Database      │            │  Object Storage    │        │  (future) CDN /   │
                               │ H2 (dev) /       │            │  Local disk (dev)  │        │  presigned URLs   │
                               │ PostgreSQL (prod)│            │  AWS S3 (prod)     │        │                    │
                               └────────────────┘            └──────────────────┘        └──────────────────┘
```

**Layering (backend):** `controller` (HTTP/DTOs) → `service` (business rules, access
control) → `repository` (Spring Data JPA) → `model` (JPA entities). Cross-cutting concerns
(`JwtAuthFilter`, `SecurityConfig`, `GlobalExceptionHandler`) live in `config` /
`exception`.

---

## 4. Database Design

| Table | Key Columns | Notes |
|---|---|---|
| `app_user` | id, name, email (unique), password_hash, role | role = `ADMIN` \| `TEAM_MEMBER` |
| `event` | id, name, admin_id, created_at | owned by exactly one Admin |
| `event_member` | id, event_id, user_id | join table; a Team Member ↔ Event assignment |
| `photo` | id, event_id, uploaded_by, filename, storage_location, file_size, content_type, selected_for_gallery, created_at | `storage_location` is a key/path, **not** the image bytes |
| `gallery` | id, event_id (unique), link_code (unique), pin_hash, published, created_at, expires_at | one gallery per event; republishing rotates link+PIN |

Relationships: `User 1—* Event` (as admin) · `Event *—* User` (via `event_member`, team
members) · `Event 1—* Photo` · `Event 1—1 Gallery` (current design; extendable to 1–many
if multiple galleries per event are needed later).

---

## 5. Security & Access Control

- Passwords and gallery PINs are BCrypt-hashed — never stored or logged in plain text.
- JWT-protected API for Admin/Team Member routes (`/api/events/**`, `/api/photos/**`);
  the gallery access route (`/api/gallery/**`) is intentionally public but gated by the
  PIN check inside `GalleryService`, matching the "no customer account" requirement.
- **Role-based authorization** enforced in `SecurityConfig` (method-level) and again in
  `EventService`/`PhotoService`/`GalleryService` (business-level), e.g.:
  - Only `ADMIN` can create events, add members, select photos, and publish galleries.
  - A Team Member can only upload/view photos for events they are explicitly assigned to.
  - `EventService.getAccessibleEvent()` rejects any user (Admin or Team Member) trying to
    touch an event they don't own/aren't assigned to → **HTTP 403**.
- **Explicitly handled edge cases** (see `GlobalExceptionHandler` + service checks):
  - A user attempting to access another event → 403 Forbidden
  - A Team Member attempting to publish a gallery → 403 Forbidden
  - A failed photo upload (bad type, empty file, oversized file) → 400 Bad Request, other
    files in the same batch still process independently
  - An incorrect gallery PIN → 401 Unauthorized
  - Attempted access to an unpublished/unselected photo → never returned by the gallery
    endpoint; only `selectedForGallery = true` photos are served publicly

---

## 6. Local Setup

### Prerequisites
- Java 17+
- Maven 3.9+ (or use the included `mvnw` if you add one)
- A modern browser (frontend needs no build tooling)

### Run the backend
```bash
cd backend
mvn spring-boot:run
```
This starts the API on **http://localhost:8080** using an in-memory H2 database and local
disk storage under `backend/uploads/`. Demo accounts are seeded automatically on startup
(see below). H2 console (dev only): http://localhost:8080/h2-console (JDBC URL
`jdbc:h2:mem:photoshare`, user `sa`, blank password).

### Run the frontend
The frontend is static HTML/CSS/JS — no build step. Simplest option:
```bash
cd frontend
python3 -m http.server 5500
```
Then open **http://localhost:5500**. It talks to the backend at `http://localhost:8080`
by default (see `js/api.js`, `API_BASE`). To point it at a different backend URL, either
edit that constant or set `window.API_BASE` before the script loads.

### Demo Credentials (seeded automatically, dev profile only)
| Role | Email | Password |
|---|---|---|
| Admin | `admin@trizen-demo.com` | `Admin@123` |
| Team Member | `member@trizen-demo.com` | `Member@123` |

### Environment Variables
| Variable | Purpose | Default |
|---|---|---|
| `APP_JWT_SECRET` | HMAC signing key for JWTs (**set a strong random value in prod**) | dev-only fallback in `application.yml` |
| `APP_PUBLIC_BASE_URL` | Base URL used to build gallery links | `http://localhost:8080` |
| `APP_STORAGE_PROVIDER` | `local` or `s3` | `local` |
| `APP_STORAGE_LOCAL_DIR` | Disk path for local storage | `./uploads` |
| `APP_S3_BUCKET` | S3 bucket name (when `APP_STORAGE_PROVIDER=s3`) | — |
| `APP_AWS_REGION` | AWS region for S3 | `ap-south-1` |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | AWS credentials (standard SDK env vars; **never commit these**) | — |
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | PostgreSQL connection (prod profile) | — |

No secrets are committed to this repository; all of the above are read from the
environment at runtime.

---

## 7. Deployment Steps (example: Render/Railway/EC2 + S3 + managed Postgres)

1. Provision a PostgreSQL instance and an S3 bucket.
2. Deploy the backend (`mvn clean package` → run the jar, or containerize with a simple
   `Dockerfile` using `eclipse-temurin:17-jre`) with `SPRING_PROFILES_ACTIVE=prod` and all
   environment variables from the table above set on the host.
3. Deploy the frontend as static files (e.g. Netlify, Vercel, S3+CloudFront, or any static
   host) and set `API_BASE` (in `js/api.js`) to the deployed backend URL.
4. Confirm CORS: `SecurityConfig` currently allows all origins for development
   (`allowedOriginPatterns("*")`) — **restrict this to your actual frontend origin before
   going live**.
5. Smoke-test the full workflow end-to-end: register → create event → add member →
   upload → select → publish → access gallery with PIN.

---

## 8. Testing

Backend integration tests (`backend/src/test/.../PhotoShareApplicationTests.java`) using
`MockMvc` cover:
- Registration + login (authentication)
- Role-based authorization (a Team Member cannot publish a gallery)
- Cross-event access denial (a user cannot view an event they don't own/aren't assigned to)
- Unauthenticated access to protected routes is rejected
- Publishing fails without any selected photos
- Gallery access with an invalid link/PIN is rejected

Run with:
```bash
cd backend
mvn test
```

---

## 9. Known Limitations

- Local disk storage (dev default) is not durable across redeploys — use the S3 provider
  for any real deployment.
- One gallery per event in the current schema (republishing rotates the same gallery's
  link/PIN rather than creating a new one); extending to multiple galleries per event is a
  straightforward schema change (drop the `event_id` unique constraint on `gallery`).
- No image thumbnailing/resizing, pagination, search, or CDN — listed as optional/bonus
  items in the spec and intentionally deprioritized in favor of the core workflow.
- The frontend is intentionally framework-free static HTML/CSS/JS per the requested stack;
  it does client-side routing via separate pages rather than a SPA router.
- `ddl-auto: update` is used for convenience in dev; production schema changes should go
  through a real migration tool (Flyway/Liquibase) rather than Hibernate auto-DDL.

---

## 10. Submission Checklist Mapping

| Deliverable | Where |
|---|---|
| Source code | This repository (`backend/`, `frontend/`) |
| Live application URL | Add after deployment |
| README | This file |
| Architecture / DB explanation | Sections 3–4 above |
| Demo credentials | Section 6 |
| Demo Gallery URL & PIN | Create an event → upload → select → publish (Section 1 workflow) to generate one |
| Tests | Section 8 |
| Deployment | Section 7 |
