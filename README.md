# FIXLOOP — Unified Campus Incident Resolution & Triage Platform

FixLoop is an enterprise-grade campus grievance management platform that provides a single point of intake for students, central triage with dynamic priority scoring for administrators, and scoped dispatch queues for workforce staff.

---

## 1. Repository Architecture

```
FIXLOOP/
├── backend/                 # Spring Boot 3.3.x (Java 21, Spring Security, JPA, WebSocket)
├── web/                     # React + TypeScript Dashboard (Admin Central Triage & Staff Queue)
├── mobile/                  # Android Student Application (Kotlin & Jetpack Compose)
├── docker/                  # Docker Compose Infrastructure (PostgreSQL 16)
├── docs/                    # Architecture Specs, ERD, API Contracts & Workflows
└── README.md
```

---

## 2. Component Directory Overview

| Directory | Stack | Description |
|---|---|---|
| [`web/`](./web) | React 18, TypeScript, Framer Motion, Recharts | Shared administrative and staff portal with role-based routing (`/admin/*`, `/staff/*`), real-time global search (`Ctrl+K`), confidential vaults (Anti-Ragging, Harassment, Malpractice), and dynamic formula inspection. |
| [`backend/`](./backend) | Spring Boot 3.3, Java 21, JPA/Hibernate, PostgreSQL, STOMP | RESTful API and WebSocket backend managing authentication, role-based JWT clearance, SLA escalation cron jobs, and intelligent dispatch. |
| [`mobile/`](./mobile) | Kotlin, Jetpack Compose, Retrofit, Room, FCM | Native Android student mobile application supporting dynamic category forms, token tracking, offline drafts, and push updates. |
| [`docker/`](./docker) | Docker Compose, PostgreSQL 16 Alpine | Local database container with persistent named volume and health checks. |
| [`docs/`](./docs) | Mermaid ERD, OpenAPI specs, System Requirements | Architectural documentation, database schema definitions, and API specifications. |

---

## 3. Dynamic Priority Scoring Engine

FixLoop computes priority through a multi-factor normalized equation:

$$\begin{aligned}
\text{severity\_norm} &= \frac{\text{severity}}{5} \\
\text{scale\_norm} &= \frac{\ln(1 + \text{students\_affected})}{\ln(1 + 500)} \quad (\text{capped at } 500 \text{ students}) \\
\text{recurrence\_norm} &= \frac{\min(\text{recurrence\_count\_last\_90\_days}, 10)}{10} \\
\text{time\_urgency\_norm} &= \min\left(\frac{\text{hours\_since\_created}}{\text{sla\_hours}}, 1.0\right) \\[8pt]
\mathbf{\text{priority\_score}} &= (0.48 \times \text{severity\_norm}) + (0.21 \times \text{scale\_norm}) \\
&\quad + (0.08 \times \text{recurrence\_norm}) + (0.23 \times \text{time\_urgency\_norm})
\end{aligned}$$

---

## 4. Quick Start Guide

### 4.1 Database (Docker)
```bash
cd docker
docker compose up -d
```
Database URL: `jdbc:postgresql://localhost:5432/fixloop_db` (user: `fixloop_user`, password: `fixloop_secret`)

### 4.2 Web Dashboard (React)
```bash
cd web
npm install
npm start
```
Runs at `http://localhost:3000`. Test accounts:
- **Admin**: `admin@fixloop.edu`
- **Staff**: `staff@fixloop.edu`

### 4.3 Backend Service (Spring Boot)
```bash
cd backend
./mvnw spring-boot:run
```
Runs at `http://localhost:8080`.

### 4.4 Mobile App (Kotlin)
Connect your Android phone wirelessly via ADB and reverse the port:
```bash
adb pair <PHONE_IP>:<PORT>
adb connect <PHONE_IP>:<PORT>
adb reverse tcp:8080 tcp:8080
```
Open `mobile/` in Android Studio and run on device.
