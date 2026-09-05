# FixLoop — Entity Relationship Diagram (ERD) & Database Schema

This document outlines the core relational database schema for FixLoop on PostgreSQL.

---

## 1. Entity Relationship Diagram

```mermaid
erDiagram
    STUDENT ||--o{ COMPLAINT : "submits"
    CATEGORY ||--o{ COMPLAINT : "classifies"
    LOCATION ||--o{ COMPLAINT : "occurs_at"
    INCIDENT ||--o{ COMPLAINT : "clusters"
    DEPARTMENT ||--o{ STAFF : "employs"
    DEPARTMENT ||--o{ COMPLAINT : "routed_to"
    STAFF ||--o{ COMPLAINT : "assigned_to"
    ADMIN ||--o{ COMPLAINT : "triages"
    VENDOR ||--o{ COMPLAINT_VENDOR : "engaged_in"
    COMPLAINT ||--o{ COMPLAINT_VENDOR : "delegates"
    COMPLAINT ||--o{ STATUS_HISTORY : "tracks"
    COMPLAINT ||--o{ ATTACHMENT : "includes"
    COMPLAINT ||--o| REVIEW : "rated_by"
    COMPLAINT ||--o{ ESCALATION_LOG : "triggers"
    CATEGORY ||--o{ SLA_RULE : "governed_by"

    STUDENT {
        uuid id PK
        varchar roll_no UK
        varchar name
        varchar department
        varchar email UK
        varchar phone
        timestamp created_at
    }

    STAFF {
        uuid id PK
        uuid department_id FK
        varchar name
        varchar role
        varchar email UK
        text[] skill_tags
        varchar status "ACTIVE | INACTIVE"
        float current_weighted_load
        timestamp created_at
    }

    ADMIN {
        uuid id PK
        varchar name
        varchar email UK
        varchar authority_level "TIER_1 | TIER_2 | CHIEF_PROCTOR"
        boolean restricted_clearance
    }

    DEPARTMENT {
        uuid id PK
        varchar name UK
        varchar code UK
        text description
        varchar head_of_dept
    }

    CATEGORY {
        uuid id PK
        varchar name UK
        varchar icon
        varchar color
        boolean sensitive "true for Harassment, Malpractice, Ragging"
        int base_severity_weight
        text[] required_skill_tags
        jsonb form_schema
        int default_sla_hours
    }

    LOCATION {
        uuid id PK
        varchar building
        varchar floor
        varchar room
        varchar campus_zone
    }

    INCIDENT {
        uuid id PK
        uuid category_id FK
        uuid location_id FK
        varchar cluster_key
        varchar status "OPEN | RESOLVED | ARCHIVED"
        int complaint_count
        timestamp created_at
        timestamp last_complaint_at
    }

    COMPLAINT {
        uuid id PK
        varchar token UK "e.g. SNT-2026-000240"
        uuid student_id FK "nullable for anonymous"
        uuid category_id FK
        uuid location_id FK
        uuid incident_id FK "nullable"
        text description
        int severity "1 to 5"
        int students_affected
        int recurrence_count_90d
        varchar status "SUBMITTED | UNDER_ADMIN_REVIEW | ASSIGNED | IN_PROGRESS | AWAITING_STUDENT_INFO | VENDOR_ASSIGNED | RESOLVED | REOPENED | CLOSED"
        float priority_score "Dynamic Multi-factor formula 0-100"
        jsonb extra_data "Category-specific dynamic form responses"
        boolean is_anonymous
        uuid assigned_by_admin_id FK "nullable"
        uuid assigned_to_staff_id FK "nullable"
        uuid routed_department_id FK "nullable"
        varchar dispatch_type "INTERNAL_STAFF | EXTERNAL_VENDOR"
        int escalation_level "1 to 3"
        boolean is_at_risk
        boolean is_breached
        timestamp created_at
        timestamp sla_deadline
        timestamp resolved_at
    }

    STATUS_HISTORY {
        uuid id PK
        uuid complaint_id FK
        varchar status
        varchar changed_by_name
        varchar changed_by_role "STUDENT | STAFF | ADMIN | SYSTEM"
        text note
        timestamp timestamp
    }

    COMPLAINT_VENDOR {
        uuid id PK
        uuid complaint_id FK
        uuid vendor_id FK
        varchar contact_status "CONTACTED | CONFIRMED | ON_SITE | DONE"
        text notes
        timestamp assigned_at
        timestamp updated_at
    }

    VENDOR {
        uuid id PK
        varchar name
        text[] category_tags
        varchar phone
        varchar email
        varchar contact_person
    }

    REVIEW {
        uuid id PK
        uuid complaint_id FK
        int rating "1 to 5 stars"
        text comment
        timestamp created_at
    }

    ATTACHMENT {
        uuid id PK
        uuid complaint_id FK
        varchar file_name
        varchar file_url
        varchar mime_type
        bigint file_size_bytes
        timestamp uploaded_at
    }

    SLA_RULE {
        uuid id PK
        uuid category_id FK
        varchar severity_tier "LOW | MEDIUM | HIGH | CRITICAL"
        int resolution_hours
        int escalation_hours
    }

    ESCALATION_LOG {
        uuid id PK
        uuid complaint_id FK
        uuid from_staff_id FK "nullable"
        uuid to_staff_id FK "nullable"
        int previous_level
        int new_level
        text reason
        timestamp timestamp
    }
```

---

## 2. Core Relational Tables & PostgreSQL Constraints

### 2.1 Dynamic Form Schema & Response Handling (`JSONB`)
Categories define custom input requirements (e.g. room number, nature of ragging, lab device) using `JSONB` in the `category.form_schema` column:
```json
[
  { "key": "raggingType", "label": "Nature of Ragging", "type": "select", "required": true, "options": ["Physical", "Verbal", "Extortion", "Forced Acts"] },
  { "key": "immediateAssistanceNeeded", "label": "Immediate Assistance Needed?", "type": "boolean", "required": true }
]
```
Submissions store actual user values in `complaint.extra_data` without requiring DDL schema alterations.

### 2.2 Dynamic Priority Score Formula Implementation
Each complaint calculates priority dynamically according to:
$$\text{severity\_norm} = \frac{\text{severity}}{5}$$
$$\text{scale\_norm} = \frac{\ln(1 + \text{students\_affected})}{\ln(1 + 500)} \quad (\text{capped at 500})$$
$$\text{recurrence\_norm} = \frac{\min(\text{recurrence\_count\_last\_90\_days}, 10)}{10}$$
$$\text{time\_urgency\_norm} = \min\left(\frac{\text{hours\_since\_created}}{\text{sla\_hours}}, 1.0\right)$$
$$\text{priority\_score} = 100 \times \Big( (0.48 \times \text{severity\_norm}) + (0.21 \times \text{scale\_norm}) + (0.08 \times \text{recurrence\_norm}) + (0.23 \times \text{time\_urgency\_norm}) \Big)$$
