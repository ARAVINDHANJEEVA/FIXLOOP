# Sentinel — Campus Complaint Resolution & Incident Management System

---

## 1. Problem Statement

Educational campuses have facilities and issues spread across multiple domains — hostel maintenance, food quality, academics, health, hygiene, IT infrastructure, and sensitive matters like harassment or malpractice. Today, students dealing with any of these issues have to figure out **who** to contact, and reach out to different people/departments separately for each type of problem — verbally, via WhatsApp groups, or paper registers.

This creates:
- No single point of contact for the student
- No accountability or measurable resolution time
- No visibility into recurring/duplicate issues
- No structured routing to the correct department or vendor
- No audit trail of who did what, and when

**Sentinel solves this by being a single platform where a student reports any issue, regardless of domain, and an admin triages and dispatches it to the correct department, staff member, or external vendor — with SLA tracking, automatic escalation, and real-time status updates back to the student.**

The core idea: **the student never needs to know who is responsible for fixing their problem.** They report it once, categorized broadly (Hostel, Food, Academic, Health, Hygiene, Tech Support, Harassment, Malpractice, etc.), and the system + admin take over routing, assignment, and resolution — including contacting external vendors/contractors (plumbers, electricians) where needed.

---

## 2. Roles

Sentinel has **three roles**, running across **two applications** (mobile app for students, one web dashboard shared by Admin and Staff with role-based views).

### 2.1 Student
- Submits complaints under a specific category
- Fills out a category-specific form (fields differ per category)
- Tracks status of their complaint via a token
- Responds when asked for more information
- Views vendor/dispatch status in simplified form
- Rates/reviews resolution, or reopens if unresolved
- Can submit Harassment/Malpractice complaints anonymously

### 2.2 Admin (Central Triage)
- Single point where **all incoming complaints across all categories** land
- Reviews and confirms/adjusts category if misclassified
- Decides dispatch: internal department vs external vendor
- Triggers auto-assignment to best-matching, least-loaded staff (system-driven, not manual per-person picking)
- Can manually reassign/override if needed (with audit log)
- Monitors SLA breaches, at-risk tickets, and escalations across the whole campus
- Views campus-wide analytics (hotspots, recurring issues, staff performance)
- Has exclusive visibility into anonymous/sensitive complaints (Harassment/Malpractice), routed to a restricted authority group, not the general staff pool

### 2.3 Department Staff / Workforce
- Sees only complaints assigned to them (via department + auto-assignment algorithm)
- Works the ticket: updates status, requests more info from student if needed
- Contacts/coordinates with external vendors (plumber, electrician, contractor) where applicable, updates vendor contact status
- Marks complaints resolved
- Does **not** see the full campus-wide queue — only their own assigned load

**Access control note:** Role separation is enforced at the backend (Spring Security, role-based JWT claims) — not just hidden in the UI. Admin and Staff share one React codebase with route/component-level gating (`/admin/*`, `/staff/*`), not separate applications.

---

## 3. System Flow

```
Student → picks category page (Hostel/Food/Academic/Health/Hygiene/Tech/Harassment/Malpractice)
        → fills category-specific dynamic form (fields/images vary per category)
        → submits → gets a token (e.g., SNT-2026-000482)
                ↓
Admin → sees complaint in central triage queue (sorted by priority score,
         duplicates auto-clustered into one incident card)
      → confirms/adjusts category, decides: internal dept fix OR external vendor
      → dispatches to department
                ↓
System → auto-assigns to best-matching, least-loaded staff within that department
          (skill match + current workload, not manual picking)
                ↓
Staff → works the ticket, may request more info from student,
         may contact/assign external vendor, updates status
                ↓
Student → gets real-time push/WebSocket updates at every status change
        → can respond to "need more info" prompts
        → on resolution: reviews/rates, or reopens if not actually fixed
                ↓
Scheduler (background) → monitors SLA deadlines continuously
                        → flags AT_RISK before breach
                        → auto-escalates to higher authority on breach
```

### Status Lifecycle
```
SUBMITTED
  → UNDER_ADMIN_REVIEW
  → ASSIGNED
  → IN_PROGRESS
  → AWAITING_STUDENT_INFO   (branch, returns to IN_PROGRESS after student responds)
  → VENDOR_ASSIGNED          (branch, if external workforce involved)
  → RESOLVED
  → REOPENED                 (if student rejects the fix)
  → CLOSED
```

---

## 4. Tech Stack

Local/demo-focused — no cloud deployment required. Everything runs on the developer's laptop; the mobile app connects wirelessly via ADB.

### 4.1 Mobile App (Student) — Kotlin
| Layer | Tech |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Networking | Retrofit + OkHttp |
| Local storage | Room (offline draft caching) |
| Push notifications | Firebase Cloud Messaging (FCM) |
| Image loading | Coil |
| Async | Kotlin Coroutines + Flow |
| Dependency Injection | Hilt |
| Backend connection (dev/demo) | `http://localhost:8080/api/` via wireless ADB (`adb pair` + `adb connect` + `adb reverse tcp:8080 tcp:8080`) |

### 4.2 Web Dashboard (Admin + Staff, role-based) — React
| Layer | Tech |
|---|---|
| Framework | React + TypeScript |
| Data fetching/caching | TanStack Query |
| Real-time | STOMP.js over SockJS (connects to Spring WebSocket) |
| Forms/validation | React Hook Form + Zod |
| UI components | Tailwind or MUI |
| Charts (analytics) | Recharts |
| Run mode | `npm run dev`, points to `http://localhost:8080/api` |

### 4.3 Backend — Spring Boot
| Layer | Tech |
|---|---|
| Framework | Spring Boot |
| Language | Java or Kotlin |
| Data access | Spring Data JPA (Hibernate) |
| Dynamic form fields | JSONB column via `@JdbcTypeCode(SqlTypes.JSON)` |
| Auth/roles | Spring Security (JWT-based, role claims: STUDENT / ADMIN / STAFF) |
| Scheduler (SLA/escalation) | Spring `@Scheduled` |
| Real-time | Spring WebSocket + STOMP |
| Validation | Jakarta Bean Validation (`@Valid`) |
| Build tool | Maven or Gradle |
| Run mode | Local, `bootRun`, port 8080 |

### 4.4 Database
| Layer | Tech |
|---|---|
| Database | PostgreSQL (local, via Docker) |
| Seed data | `data.sql` for demo-ready seeded complaints |
| Image storage | Local filesystem path reference (no cloud storage needed for demo) |

### 4.5 Dev/Demo Tooling
| Purpose | Tech |
|---|---|
| Wireless phone↔laptop connection | `adb pair` + `adb connect` + `adb reverse tcp:8080 tcp:8080` |
| Version control | Git + GitHub |

### 4.6 Explicitly Excluded (not needed for local demo)
Railway/Render, Vercel/Netlify, AWS S3/Cloudflare R2, Supabase, ngrok, any cloud deployment — all deferred to a future production phase.

---

## 5. Core Backend Algorithms

### 5.1 Priority Scoring
```
priority_score = (w1 * severity)
                + (w2 * log(1 + students_affected))
                - (w4 * hours_since_created / sla_hours)
                + (w3 * recurrence_count_last_90_days)
```
Weights stored in a config table, tunable after real usage — not hardcoded.

### 5.2 Incident Clustering (Deduplication)
Rule-based, not ML:
```
match if:
  same category_id
  AND same location_id (or same building if room-level too strict)
  AND created_at within X hours of an existing OPEN incident
→ attach to incident, increment complaint_count
else → create new incident
```
Runs synchronously on complaint creation. **Sensitive categories (Harassment/Malpractice) are excluded from clustering entirely.**

### 5.3 SLA Scheduler
Spring `@Scheduled` job, runs every N minutes:
```sql
SELECT * FROM complaint
WHERE status != RESOLVED
  AND sla_deadline < NOW() + at_risk_buffer
```
- Inside buffer window → flag `AT_RISK`, notify assigned staff
- Past deadline → auto-escalate to next authority level, log to `Escalation_Log`, notify

### 5.4 Auto-Assignment (Workforce Dispatch)
Triggered synchronously when admin confirms department-level dispatch:
```sql
SELECT staff.*
FROM staff
WHERE staff.department_id = :assigned_department_id
  AND staff.skill_tags @> ARRAY[:category_required_skill]
  AND staff.status = 'ACTIVE'
ORDER BY staff.current_weighted_load ASC
LIMIT 1
```
Fallback chain if no exact skill match:
```
1. Exact skill match, least loaded
2. Any staff in department, least loaded
3. No match found → flag for admin manual assignment (never silently unassigned)
```
`current_weighted_load` = sum of `priority_score` of all currently open complaints for that staff member, updated incrementally on assign/resolve.

---

## 6. Data Model (Core Entities)

```
Student(id, name, roll_no, dept, contact)
Staff(id, name, role, department_id, skill_tags[], status, current_weighted_load)
Admin(id, name, contact, authority_level)
Category(id, name, base_severity_weight, required_skill_tags[], sensitive: boolean, form_schema JSONB)
Location(id, building, floor, room)
Vendor(id, name, category_tags[], phone, email)

Complaint(id, student_id, category_id, location_id, description,
          severity, students_affected, status, priority_score,
          extra_data JSONB,                  -- category-specific fields
          is_anonymous: boolean,
          incident_id,
          assigned_by_admin_id, assigned_to_staff_id,
          dispatch_type ENUM(INTERNAL_STAFF, EXTERNAL_VENDOR),
          created_at, sla_deadline, resolved_at)

Incident(id, category_id, location_id, cluster_key, status, complaint_count, created_at)

SLA_Rule(id, category_id, severity_tier, resolution_hours, escalation_hours)
Escalation_Log(id, complaint_id, from_staff_id, to_staff_id, reason, timestamp)
Status_History(id, complaint_id, status, changed_by, note, timestamp)
Complaint_Vendor(complaint_id, vendor_id, assigned_at, contact_status, notes)
Review(id, complaint_id, rating, comment, created_at)
Attachment(id, complaint_id, file_path)
```

---

## 7. Features

### 7.1 Student App (Kotlin)

**Auth & Onboarding**
- College-email/roll-no based login/signup
- Profile with pre-filled hostel block/room for faster location tagging

**Complaint Submission**
- Category selection screen (Academic, Hostel, Food, Health, Hygiene, Tech Support, Harassment, Malpractice)
- Dynamic form per category (JSON-schema driven — different fields, optional/required image per category)
- Location tagging (auto-filled where possible)
- Multi-image upload where category allows
- Local draft auto-save (Room) — survives app kill
- Anonymous submission toggle (Harassment/Malpractice only)

**Token & Status Tracking**
- Auto-generated readable token on submit
- Visual status timeline (stepper), not just a text label
- "My Complaints" list, filterable by status
- Expandable status history per complaint

**Real-Time Updates**
- FCM push on every status change
- `AWAITING_STUDENT_INFO` — actionable in-app prompt (reply/upload directly), not just a notification
- Live refresh while app is foregrounded (WebSocket), FCM otherwise

**Vendor/Dispatch Visibility**
- Simplified, human-readable dispatch status ("Assigned to Electrical Dept," "Plumber contacted — ETA today")

**Review & Feedback**
- Post-resolution rating + comment (once, after RESOLVED)
- Reopen action if unresolved — re-enters queue instead of new submission

**Notifications Center**
- In-app history of all notifications, not just push

### 7.2 Admin Dashboard (React, `/admin/*`)
- Central triage queue — all complaints, all categories, sorted by priority score
- Clustered incident cards (duplicate complaints grouped, with count badge)
- Category confirm/override
- Dispatch controls: route to department or external vendor
- Manual reassignment override (with audit log) after auto-assignment
- Campus-wide SLA monitoring — at-risk and breached tickets
- Escalation visibility
- Campus-wide analytics: hotspot analysis (location × category), staff performance, SLA breach trends, seasonal patterns
- Exclusive access to anonymous/sensitive complaints, routed to restricted authority view only

### 7.3 Staff Dashboard (React, `/staff/*`)
- Personal assigned-ticket queue only (not campus-wide)
- Status update controls (IN_PROGRESS, request more info, resolve)
- Vendor assignment + contact status update
- View of their own current workload

---

## 8. Features Deliberately Excluded from v1 / Demo

| Excluded | Reason |
|---|---|
| In-app chat with staff | `AWAITING_STUDENT_INFO` + reply covers the "ask for more info" need without full chat infra |
| Map view of hotspots on student side | This is an admin analytics feature, not a student need |
| Multi-language support | Not core to proving the architecture works |
| Cloud deployment | Demo runs fully local; deployment is a future phase |
| ML-based clustering/prediction | Rule-based clustering is sufficient at this data volume; premature ML is wasted effort |

---

## 9. Demo Setup Summary

1. PostgreSQL running locally via Docker
2. Spring Boot running locally (port 8080)
3. React dashboard running locally (`npm run dev`), pointed at `localhost:8080/api`
4. Kotlin app connected wirelessly via `adb pair` + `adb connect` + `adb reverse tcp:8080 tcp:8080`, using `http://localhost:8080/api/` as base URL
5. Seeded demo data (`data.sql`) — realistic complaints across categories, including one AT_RISK and one already RESOLVED, so dashboards aren't empty
6. Demo flow: student submits complaint → appears live on admin dashboard (WebSocket) → admin dispatches → auto-assignment picks staff → staff updates status → student receives real-time push → student reviews/reopens

This single loop demonstrates the full end-to-end value proposition: **one submission point, centralized triage, automated intelligent routing, and real-time visibility for the student — without deployment infrastructure.**