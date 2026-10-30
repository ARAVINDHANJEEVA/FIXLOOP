# FIXLOOP — Unified Campus Incident Resolution & Triage Platform

FixLoop is an enterprise-grade campus grievance and incident management platform that provides a single point of intake for students, central triage with dynamic priority scoring for administrators, and scoped dispatch queues for workforce staff.

---

## 1. System Architecture & Directory Structure

```
FIXLOOP/
├── backend/                 # Spring Boot 3.3.x (Java 21, Spring Security, JPA/Hibernate, WebSocket)
├── web/                     # React 18 + TypeScript Dashboard (Admin Central Triage & Staff Queue)
├── mobile/                  # Android Student Application (Kotlin & Jetpack Compose, Material 3)
├── docker/                  # Docker Compose Infrastructure (PostgreSQL 16 Alpine)
│   ├── docker-compose.yml
│   └── .env.example
├── mdns_publisher.py        # Zeroconf mDNS publisher for automatic mobile LAN discovery
├── start-mdns.bat           # 1-click launcher for mDNS publisher (no manual IP required)
├── run-all.bat / .ps1       # 1-click launcher for Full Stack (PostgreSQL + Backend + Frontend)
├── run-backend.bat / .ps1   # 1-click launcher for Backend + Database only
├── stop-all.bat / .ps1      # 1-click script to safely terminate all FixLoop services
└── README.md
```

---

## 2. Technology Stack

| Subsystem | Technologies & Libraries | Key Features |
|---|---|---|
| **Backend API** | Java 21, Spring Boot 3.3.3, Spring Security 6, Spring Data JPA, Flyway, PostgreSQL | Stateless JWT authentication, role-based access control, dynamic priority calculation, TF-IDF cosine similarity clustering, SLA deadline monitoring cron jobs, and STOMP WebSocket broadcasting. |
| **Web Dashboard** | React 18, TypeScript, Tailwind CSS, Lucide Icons, Recharts, Framer Motion | Role-based portal (`/admin/*`, `/staff/*`), real-time queue synchronization, multi-attribute filter group, cluster inspection panel, and dynamic priority weight tuning. |
| **Mobile App** | Kotlin 1.9, Jetpack Compose, Retrofit 2, OkHttp 3, Coroutines & Flow | Neo-brutalism design, zero-config mDNS backend discovery, student authentication, offline draft support, camera photo attachments, and real-time status timelines. |
| **Infrastructure** | Docker, Docker Compose, PostgreSQL 16 | Containerized relational database with persistent named volumes and healthcheck probe. |

---

## 3. Dynamic Priority Scoring Engine

FixLoop automatically ranks campus grievances using a normalized multi-factor priority algorithm:

$$\begin{aligned}
\text{severity}_{\text{norm}} &= \frac{\text{severity}}{5} \\
\text{scale}_{\text{norm}} &= \frac{\ln(1 + \text{students affected})}{\ln(1 + 500)} \quad (\text{capped at } 500 \text{ students}) \\
\text{recurrence}_{\text{norm}} &= \frac{\min(\text{recurrence count (90 days)}, 10)}{10} \\
\text{urgency}_{\text{norm}} &= \min\left(\frac{\text{hours elapsed}}{\text{SLA hours}}, 1.0\right) \\[8pt]
\mathbf{\text{Priority Score}} &= (0.48 \times \text{severity}_{\text{norm}}) + (0.21 \times \text{scale}_{\text{norm}}) \\
&\quad + (0.08 \times \text{recurrence}_{\text{norm}}) + (0.23 \times \text{urgency}_{\text{norm}})
\end{aligned}$$

Weights ($w_1=0.48, w_2=0.21, w_3=0.08, w_4=0.23$) can be adjusted in real-time by administrators via the Web Settings panel.

---

## 4. Quick Start (Windows Convenience Scripts)

FixLoop includes automated scripts in the root directory for fast local development:

| Script | Purpose |
|---|---|
| `run-all.bat` / `.\run-all.ps1` | Launches PostgreSQL in Docker, starts the Spring Boot backend on `:8080`, and runs the React frontend on `:3000` in dedicated windows. |
| `run-backend.bat` / `.\run-backend.ps1` | Ensures PostgreSQL container is healthy and starts the backend service. |
| `stop-all.bat` / `.\stop-all.ps1` | Stops processes listening on ports `8080` and `3000` and shuts down the PostgreSQL container. |
| `start-mdns.bat` | Starts the mDNS daemon so physical Android phones automatically resolve `http://fixloop.local:8080/`. |

---

## 5. Manual Setup & Running Instructions

### 5.1 Prerequisites
- **Java Development Kit (JDK)**: Version 21 (or Android Studio JBR)
- **Node.js**: Version 18+ and `npm`
- **Docker Desktop**: Running and configured for Linux containers
- **Android Studio**: Ladybug / Koala (for building the mobile app)
- **Python 3.9+**: Optional, only required for running `mdns_publisher.py`

### 5.2 Step 1: Database Setup (Docker)
```bash
cd docker
docker compose up -d
```
- **Host**: `localhost:5432`
- **Database**: `fixloop_db`
- *(Connection credentials for database inspection tools like pgAdmin/DBeaver are defined in `docker/.env.example`)*

### 5.3 Step 2: Backend Service (Spring Boot)
```bash
cd backend
# On Windows
mvnw.cmd spring-boot:run
# On Linux/macOS
./mvnw spring-boot:run
```
- The backend will automatically apply database migrations via Flyway and seed initial administrative categories and accounts.
- Base URL: `http://localhost:8080`
- Health check: `http://localhost:8080/api/health`

### 5.4 Step 3: Web Dashboard (React)
```bash
cd web
npm install
npm start
```
- Base URL: `http://localhost:3000`

### 5.5 Step 4: Mobile Application (Android)

Open the `mobile/` directory in Android Studio.

#### Connecting the Mobile App to the Backend:
1. **Android Emulator**:
   - Select **Emulator Mode** in the app settings screen.
   - Automatically connects to `http://10.0.2.2:8080/`.
2. **Physical Device over Wi-Fi (Zero-Configuration with mDNS)**:
   - Run `start-mdns.bat` (or `python mdns_publisher.py`) on your development machine.
   - Ensure phone and laptop are on the same Wi-Fi / hotspot.
   - The app will resolve the backend at `http://fixloop.local:8080/`.
3. **Physical Device via USB ADB Reverse**:
   ```bash
   adb reverse tcp:8080 tcp:8080
   ```
   - Connects to `http://localhost:8080/`.

---

## 6. Seed Accounts & Default Credentials

| Role | Email | Password | Access Scope |
|---|---|---|---|
| **Administrator** | `admin@fixloop.edu` | `admin123` | Full dashboard access, dynamic weights, staff management, cluster actions, and analytics. |
| **Workforce Staff** | `staff@fixloop.edu` | `staff123` | Scoped department queues, status updates, photo proof uploads, and work logs. |
| **Student** | Dynamic / `student@fixloop.edu` | `student123` | Grievance intake, tracking, timeline updates, feedback, and reviews. |

---

## 7. Production Deployment & Security

- **Environment Configuration**: Copy `docker/.env.example` to `docker/.env` and configure strong passwords and a secure JWT secret:
  ```bash
  POSTGRES_DB=fixloop_db
  POSTGRES_USER=fixloop_user
  POSTGRES_PASSWORD=your_strong_db_password
  JWT_SECRET=your_base64_encoded_256_bit_secret
  ```
- **Protected Exclusions**: All `.env` files, internal architecture plans (`plan/`, `plans/`), private keys (`*.pem`, `*.key`, `*.jks`), IDE configurations, and internal developer documentation are excluded from version control via `.gitignore`.
- **Stateless Authentication**: Microservice-ready stateless JWT validation with BCrypt password hashing.

---

## 8. License

This project is licensed under the GNU General Public License v3.0 (GNU GPLv3) — see the [`LICENSE`](./LICENSE) file for details.
