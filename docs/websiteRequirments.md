# Sentinel — Web Dashboard Requirements (Admin + Staff)

React + TypeScript single application, role-gated at route and component level. Backend: Spring Boot REST + WebSocket (STOMP/SockJS), running at `http://localhost:8080/api` for local demo.

---

## 1. Roles & Access Control

| Role | Route prefix | JWT claim required |
|---|---|---|
| Admin | `/admin/*` | `ROLE_ADMIN` |
| Restricted Authority (subset of Admin) | `/admin/restricted` | `ROLE_ADMIN` + `restricted_access: true` |
| Staff | `/staff/*` | `ROLE_STAFF` |

- Route guarding done via a `ProtectedRoute` wrapper component checking JWT role claim client-side, **but this is UX only** — every actual data-fetch/action must be independently authorized server-side via Spring Security (`@PreAuthorize`). Never trust client-side gating alone.
- JWT stored in memory + refresh token flow (avoid localStorage for the access token if you want to reduce XSS exposure; acceptable to relax this for a demo build, but should be noted as a known simplification).
- Single login screen for both roles; redirect target determined by role claim after successful auth.

---

## 2. Global App Shell

**Components used across every authenticated page:**
- `AppHeader` — logo, role badge, current user name, notification bell, logout
- `SideNav` — role-specific nav items (Admin sees Queue/SLA/Analytics/Restricted/Staff Mgmt/Settings; Staff sees Queue/Workload only)
- `NotificationBell` — WebSocket-driven live badge count, dropdown preview of latest 5 events
- `ToastContainer` — non-blocking real-time alerts ("New complaint received," "Ticket assigned to you")
- `StatusBadge` — reusable color-coded pill: `SUBMITTED` (gray), `UNDER_ADMIN_REVIEW` (blue), `ASSIGNED` (indigo), `IN_PROGRESS` (amber), `AWAITING_STUDENT_INFO` (orange), `VENDOR_ASSIGNED` (purple), `RESOLVED` (green), `REOPENED` (red), `CLOSED` (slate)
- `LoadingSkeleton` — used on every data-fetching view instead of spinners for perceived performance
- `EmptyState` — shown when a filtered queue returns zero results (avoid a blank white screen)
- `ErrorBoundary` — wraps route content, catches render errors without crashing the whole app

---

## 3. WebSocket / Real-Time Layer

- Single STOMP client instance established on login, connected via SockJS to Spring's `/ws` endpoint
- Subscribe topics:
    - `/topic/admin/queue` — new complaint submitted or reclassified (Admin only)
    - `/topic/staff/{staffId}/queue` — new assignment, reassignment (Staff, user-specific)
    - `/topic/sla/alerts` — at-risk/breach events (Admin)
    - `/topic/complaint/{complaintId}` — status change on a specific complaint (subscribed only while viewing that Detail page)
- Reconnect logic: exponential backoff, resubscribe to prior topics on reconnect
- Incoming WebSocket events update TanStack Query cache directly (`queryClient.setQueryData` / `invalidateQueries`) rather than maintaining a separate real-time state store — avoids two sources of truth for the same data
- Disconnect cleanly on logout/unmount to avoid leaked connections

---

## 4. Admin Pages — Detailed Requirements

### 4.1 Login (`/login`)
- Fields: email, password
- Client-side validation (Zod schema): required, email format
- On success: store JWT, redirect based on role claim
- Error state: invalid credentials message (no distinction between "wrong email" vs "wrong password" — avoid user enumeration)

### 4.2 Triage Queue (`/admin/queue`)
**Purpose:** primary working view for Admin, default landing page post-login.

**Data requirements:**
- Paginated list of complaints/incidents, default sort `priority_score DESC`
- Real-time insertion of new complaints via WebSocket without full page refetch

**UI elements:**
- Filter bar: category (multi-select), status (multi-select), department, date range picker, priority score range slider
- Sort control: Priority / Newest / SLA Deadline (ascending)
- View toggle: Table view vs Card view (card view better surfaces clustered incidents visually)
- Incident cluster indicator: complaints belonging to the same `incident_id` collapse into one row/card with a count badge (e.g., "14 reports") and an expand chevron to view individual complaints within
- Row/card fields: token, category icon, short description snippet, location, priority score (numeric + visual indicator, e.g., colored dot for high/med/low), time since submission, current status badge
- Pagination or infinite scroll (infinite scroll preferred for a triage-style queue)
- Row click → navigates to Complaint Detail
- Bulk actions (optional, v2): multi-select rows → bulk category reassign (not essential for demo)

### 4.3 Complaint Detail (`/admin/complaint/:id`)
**Data requirements:**
- Fetch full complaint by ID including `extra_data` JSONB, attachments, student info (or "Anonymous"), status history, clustered sibling complaints if part of an incident

**UI sections:**
1. **Header**: token, category, current status badge, priority score, created timestamp
2. **Submitted content**: description, all dynamic fields rendered from `extra_data` against the category's `form_schema` (labels, not raw JSON keys), image gallery (lightbox on click)
3. **Location**: building/floor/room, optionally rendered on a simple campus map graphic (skip for demo unless time allows)
4. **Dispatch panel**:
    - Category confirm/override dropdown (pre-filled with submitted category)
    - Dispatch type radio: Internal Staff / External Vendor
    - If Internal: "Auto-Assign" button → calls assignment endpoint → displays resulting staff name, department, current load, with a manual "Reassign" fallback (searchable staff dropdown, filtered by department)
    - If Vendor: searchable vendor dropdown (filtered by `category_tags`), contact notes textarea
    - Reassign action requires a reason field (free text, mandatory) → written to `Escalation_Log`/audit trail
5. **Status history**: vertical timeline, each entry: status, changed_by, timestamp, optional note
6. **Related complaints**: if part of a clustered incident, list of sibling complaint tokens with mini status badges, clickable
7. **SLA info**: deadline countdown, escalation level if any

### 4.4 SLA Monitor (`/admin/sla`)
- Two-panel or tabbed layout: **At-Risk** | **Breached**
- Table columns: token, category, assigned staff/department, time remaining (At-Risk) or overdue duration (Breached), current escalation level
- Row action: "Force Escalate" button (manual override of the automated scheduler) with confirmation dialog
- Auto-refresh via WebSocket `/topic/sla/alerts` — new at-risk/breach events append live without manual refresh
- Sort by time-remaining ascending by default (most urgent first)

### 4.5 Analytics Dashboard (`/admin/analytics`)
- **Hotspot chart**: bar or heatmap, complaints grouped by location × category, date-range filterable
- **SLA breach trend**: line chart, breach count over time (weekly/monthly buckets)
- **Category volume**: pie or horizontal bar, complaint count per category over selected period
- **Staff performance table**: staff name, department, avg resolution time, SLA compliance %, current open ticket count — sortable columns
- **Recurring issue flags**: list/card of locations that crossed the recurrence threshold (e.g., 3+ incidents/month same category), each with a "flag for preventive inspection" action
- All charts built with Recharts, data fetched via dedicated aggregate endpoints (`/api/analytics/hotspots`, `/api/analytics/sla-trends`, etc.) — do not compute aggregates client-side from raw complaint lists

### 4.6 Restricted / Sensitive Complaints (`/admin/restricted`)
- Access gated by `restricted_access` claim in addition to `ROLE_ADMIN`
- Queue identical in structure to main Triage Queue but:
    - No clustering applied
    - No public/aggregate counts shown anywhere
    - Category limited to Harassment, Malpractice
    - Dispatch panel routes only to a fixed, pre-configured authority group — no department/vendor selection
- Anonymous complaints show "Anonymous Student" instead of identity, even to restricted admins, unless a specific "reveal identity" action exists with its own audit-logged justification (only include this if genuinely required — default to never revealing)

### 4.7 Staff & Department Management (`/admin/staff`)
- Table: staff name, department, skill tags, status (active/inactive toggle), current weighted load
- Add/Edit staff modal: name, department dropdown, skill tags (multi-select or tag input), status
- Department list (simple CRUD — name, description)
- Not the highest priority page for a demo; a seeded read-only-looking table is acceptable if time-constrained

### 4.8 Category & SLA Settings (`/admin/settings`)
- Category list: name, sensitive flag toggle, base severity weight, required skill tags
- Form schema editor — for a demo, a JSON textarea with validation is acceptable; a full drag-and-drop form builder is out of scope
- SLA rules table: category + severity tier → resolution hours, escalation hours (editable inline)
- Priority score weight config: four numeric inputs (w1–w4) with a short explanation of each, live preview of how they affect a sample score (nice-to-have, not required)

---

## 5. Staff Pages — Detailed Requirements

### 5.1 My Queue (`/staff/queue`)
- Data scoped server-side to `assigned_to_staff_id = current_user` — never filter client-side for security
- Tab/filter chips: Assigned / In Progress / Awaiting Info / Vendor Assigned / Resolved
- Sort: Priority / SLA deadline
- Row fields: token, category, short description, location, priority indicator, SLA countdown, status badge
- Real-time insertion via `/topic/staff/{staffId}/queue` when admin assigns a new ticket — toast notification + row appears without refresh

### 5.2 Complaint Detail (`/staff/complaint/:id`)
- Same read layout as Admin detail view (dynamic fields, images, location) minus dispatch controls
- **Status update controls**: button group — Start Progress, Request More Info, Mark Resolved (each triggers a confirmation or a small form, not a blind status flip)
    - "Request More Info" opens a text field for the specific question sent to the student, sets status to `AWAITING_STUDENT_INFO`
    - "Mark Resolved" requires a resolution notes field (mandatory, min length) before allowing submission
- **Vendor section**: assign vendor (dropdown filtered by category), contact status updater (Contacted → Confirmed → On-site → Done), notes field, timestamp of last update
- Status history (read-only timeline, same component as Admin view)

### 5.3 My Workload (`/staff/workload`)
- Summary cards: current open tickets, current weighted load score, tickets resolved this week, average resolution time (personal)
- Optional simple bar chart: tickets by status
- Lowest priority page to build — cut first if time-constrained

---

## 6. Shared Components Library

| Component | Used in |
|---|---|
| `ComplaintCard` / `ComplaintRow` | Admin Queue, Staff Queue |
| `StatusBadge` | Everywhere status is shown |
| `StatusTimeline` | Admin Detail, Staff Detail |
| `DynamicFieldRenderer` | Detail pages, renders `extra_data` against `form_schema` as labeled read-only fields |
| `ImageGallery` / `Lightbox` | Detail pages |
| `PriorityIndicator` | Queue rows, Detail header |
| `SLACountdown` | Queue rows, SLA Monitor, Detail header |
| `NotificationBell` + dropdown | Global header |
| `ConfirmDialog` | Any destructive/irreversible action (reassign, force-escalate, resolve) |
| `SearchableDropdown` | Vendor picker, staff reassign picker |

---

## 7. State & Data Layer

- **TanStack Query** for all server state — queue lists, complaint detail, analytics data. No separate global state manager (Redux/Zustand) needed unless auth/session state grows complex enough to justify it.
- Query keys structured hierarchically: `['complaints', filters]`, `['complaint', id]`, `['analytics', 'hotspots', dateRange]` — enables targeted invalidation on WebSocket events.
- Mutations (dispatch, reassign, status update) use `useMutation` with optimistic updates for status changes where safe (e.g., marking resolved), and cache invalidation for anything server-computed (e.g., auto-assignment result, priority score recalculation).
- Form state: React Hook Form + Zod schemas per form (login, dispatch, reassign, resolve). Validation errors shown inline, not as toast-only.

---

## 8. API Endpoints Required (minimum set)

```
POST   /api/auth/login
GET    /api/complaints?filters...          (Admin queue)
GET    /api/complaints/mine                (Staff queue, scoped server-side)
GET    /api/complaints/:id
POST   /api/complaints/:id/dispatch        { departmentId | vendorId, dispatchType }
POST   /api/complaints/:id/assign          (triggers auto-assignment algorithm)
POST   /api/complaints/:id/reassign        { staffId, reason }
PATCH  /api/complaints/:id/status          { status, note }
POST   /api/complaints/:id/request-info    { message }
POST   /api/complaints/:id/vendor          { vendorId, contactStatus, notes }
GET    /api/complaints/:id/history
GET    /api/sla/at-risk
GET    /api/sla/breached
POST   /api/sla/:id/force-escalate
GET    /api/analytics/hotspots
GET    /api/analytics/sla-trends
GET    /api/analytics/category-volume
GET    /api/analytics/staff-performance
GET    /api/staff
POST   /api/staff
PATCH  /api/staff/:id
GET    /api/categories
PATCH  /api/categories/:id
GET    /api/sla-rules
PATCH  /api/sla-rules/:id
```

---

## 9. Priority for Demo Build

| Priority | Pages/Features |
|---|---|
| **Must-have** | Login, Triage Queue, Complaint Detail (Admin, with dispatch + auto-assign), My Queue (Staff), Complaint Detail (Staff, with status update), real-time WebSocket updates on both |
| **Should-have** | SLA Monitor, basic Analytics (2–3 charts), Restricted complaints view |
| **Can defer/simplify** | Staff & Department Management (seeded table acceptable), Category/SLA Settings UI (seed via `data.sql` instead), My Workload page, form schema editor (raw JSON acceptable), bulk actions |

---

## 10. Explicit Non-Requirements for This Build

- No client-side-only role security (must always be backend-enforced)
- No Redux/global state manager unless justified by growth
- No drag-and-drop form builder for category schemas
- No production deployment concerns (hosting, CDN, SSL) — local demo only, per current project scope