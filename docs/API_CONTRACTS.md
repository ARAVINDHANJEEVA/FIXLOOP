# FixLoop — Backend API Contracts & WebSocket Specifications

Base URL: `http://localhost:8080/api`
WebSocket Endpoint: `ws://localhost:8080/ws` (STOMP over SockJS)

---

## 1. Authentication & Security Endpoints

### `POST /api/auth/login`
Authenticates a user (Student, Staff, or Admin) and returns a JWT token.
- **Request Body**:
  ```json
  {
    "email": "admin@fixloop.edu",
    "password": "secretpassword"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": "adm-1",
      "name": "Krishnan Ramaswamy",
      "email": "admin@fixloop.edu",
      "role": "ADMIN",
      "restrictedClearance": true,
      "avatarInitials": "KR"
    }
  }
  ```

---

## 2. Complaint Lifecycle & Triage Endpoints

### `POST /api/complaints`
Student creates a new issue (disclosed or anonymous).
- **Request Body**:
  ```json
  {
    "categoryId": "cat-9",
    "location": {
      "building": "Block C Hostel",
      "floor": "2nd Floor",
      "room": "C-214"
    },
    "description": "First-year hostel intimidation reported after 11 PM.",
    "severity": 5,
    "studentsAffected": 4,
    "isAnonymous": true,
    "extraData": {
      "raggingType": "Forced Acts / Tasks",
      "immediateAssistanceNeeded": false
    },
    "attachments": []
  }
  ```
- **Response `201 Created`**:
  ```json
  {
    "id": "cmp-022",
    "token": "SNT-2026-000240",
    "status": "SUBMITTED",
    "priorityScore": 61.1,
    "createdAt": "2026-09-05T17:22:00.000Z",
    "slaDeadline": "2026-09-06T05:22:00.000Z"
  }
  ```

### `GET /api/complaints`
Retrieves paginated complaints with role-based filtering:
- **Admin**: All complaints (restricted complaints requiring `restrictedClearance=true`).
- **Staff**: Only complaints assigned to the authenticated staff member.
- **Query Params**:
  - `page`: default `0`
  - `size`: default `20`
  - `status`: `ALL | SUBMITTED | IN_PROGRESS | RESOLVED...`
  - `category`: Category ID or Name
  - `search`: Full-text search (token, description, venue, student)
  - `restricted`: `true` to query the confidential vault (Admin only)

### `GET /api/complaints/{id}`
Fetches complaint detail by UUID or Token.
- **Response `200 OK`**:
  Full complaint object with status history, dynamic form fields, priority breakdown, and attachments.

### `PATCH /api/complaints/{id}/dispatch`
Admin triages and assigns complaint to internal department staff or external vendor.
- **Request Body**:
  ```json
  {
    "dispatchType": "INTERNAL_STAFF",
    "assignedStaffId": "stf-7",
    "departmentId": "dept-4",
    "note": "Assigned to Anti-Ragging squad head"
  }
  ```

### `PATCH /api/complaints/{id}/status`
Staff or Admin updates complaint progress.
- **Request Body**:
  ```json
  {
    "status": "IN_PROGRESS",
    "note": "Patrol dispatched to Block C"
  }
  ```

### `POST /api/complaints/{id}/request-info`
Staff requests clarification from the student (`AWAITING_STUDENT_INFO`).

---

## 3. Real-Time WebSocket Topics (STOMP)

- `/topic/complaints`: Global broadcast of new submissions and priority changes.
- `/topic/complaints/{token}`: Live token lifecycle updates for individual student tracking.
- `/topic/staff/{staffId}`: Direct ticket assignments and task notifications.
- `/topic/admin/sla-alerts`: Real-time notifications for tickets entering `AT_RISK` or `BREACHED` status.
