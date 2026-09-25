# 🛡️ SkySentinel Security

> **See sooner. Respond smarter.**

SkySentinel Security is a **drone-assisted security operations platform** designed to help private security companies manage incidents, panic alerts, response teams, protected sites, and optional drone observation from one centralized control room.

The platform combines traditional security response with modern situational-awareness technology while keeping **human operators in control of operational decisions**.

---

## 📌 Problem Statement

Private security companies commonly rely on security guards, patrol vehicles, motorcycles, panic buttons, alarms, and fixed CCTV cameras.

When an incident occurs, the control room may know **where an alert came from**, but may not immediately know **what is happening at the location**.

This can result in response teams travelling toward an incident with limited situational information.

Fixed CCTV cameras may also have blind spots or may not provide the required view of an incident.

**SkySentinel Security aims to improve situational awareness and incident coordination by bringing alerts, human response teams, protected sites, and optional drone observation into one security operations platform.**

---

## 🎯 Project Goal

The goal of SkySentinel is to provide security companies with a centralized **Security Operations Centre dashboard** where operators can:

- Receive panic and alarm events
- Monitor active incidents
- View protected sites on a map
- Acknowledge incidents
- Dispatch security response teams
- Monitor response-team status
- Manage drone bases
- Monitor drone availability
- Create human-authorized observation missions
- View drone telemetry
- Manage evidence
- Review incident history
- Generate reports
- Maintain an audit trail

SkySentinel is not intended to replace security officers.

The platform is designed to **support human security teams with better information and coordination**.

---

## 🖥️ Security Operations Centre

The main SkySentinel dashboard provides one operational view of the security environment.

```text
┌──────────────────────────────────────────────────────────────┐
│                 SKYSENTINEL SECURITY                        │
│              SECURITY OPERATIONS CENTRE                     │
├──────────────────────────────────────────────────────────────┤
│ Active       Drones       Missions      Response      Sites │
│ Incidents    Available    Active        Teams               │
├──────────────┬──────────────────────────┬────────────────────┤
│              │                          │                    │
│    LIVE      │        LIVE MAP          │ SELECTED INCIDENT  │
│  INCIDENTS   │                          │                    │
│              │   🚁   🔴   🚓   🏭     │ Acknowledge        │
│              │                          │ Create Mission     │
│              │                          │ Dispatch Team      │
├──────────────┼──────────────┬───────────┼────────────────────┤
│ DRONE FLEET  │ MISSIONS     │ RESPONSE  │ LIVE DRONE FEED    │
│              │              │ TEAMS     │                    │
├──────────────┴──────────────┴───────────┴────────────────────┤
│ Activity      Alerts       Weather        Quick Actions      │
└──────────────────────────────────────────────────────────────┘
```

---

## 🚨 Incident Workflow

A typical SkySentinel incident follows this workflow:

```text
Panic Button / Alarm / Manual Report
                 │
                 ▼
         SkySentinel API
                 │
                 ▼
         Incident Created
                 │
                 ▼
       Control Room Alerted
                 │
                 ▼
      Operator Acknowledges
                 │
          ┌──────┴──────┐
          │             │
          ▼             ▼
   Dispatch Team    Check Available
        🚓           Drone Resources
                          🚁
                          │
                          ▼
                Operator Authorizes
                 Observation Mission
                          │
                          ▼
                  Live Telemetry
                          │
          ┌───────────────┘
          ▼
     Incident Updated
          │
          ▼
       Resolved
          │
          ▼
 Report + Audit History
```

A panic event **does not automatically launch a drone**.

The control-room operator first reviews the incident and determines the appropriate response.

---

## 🚁 Hybrid Drone Deployment

SkySentinel supports different drone deployment models.

### Shared Drone Base

Multiple nearby protected sites can be associated with a shared drone base where such an operating model is technically and operationally appropriate.

```text
Warehouse A ──┐
              │
Warehouse B ──┼──── 🚁 Shared Drone Base
              │
Warehouse C ──┘
```

### Dedicated Drone Base

Large or high-priority facilities can have dedicated drone resources.

```text
Large Facility
      │
      └──── 🚁 Dedicated Drone Base
```

### No-Drone Site

A protected site does not need drone coverage to use SkySentinel.

```text
Protected Site
      │
      ▼
   Incident
      │
      ▼
Control Room
      │
      ▼
🚓 Response Team
```

This allows SkySentinel to support a **hybrid security model** instead of requiring every customer site to have its own drone.

---

## 🗺️ Operational Areas

SkySentinel groups protected sites and drone resources into **Operational Areas**.

Example:

```text
Industrial Area North
│
├── Warehouse A
├── Warehouse B
├── Warehouse C
│
└── Drone Base DB-001
    ├── SS-001
    └── SS-002
```

The software does not assume that every drone can operate within a fixed distance.

Actual real-world drone operations depend on factors such as aircraft capability, communications, environment, weather, airspace, and applicable operational authorization.

---

## 🚁 Drone Simulation

Physical drones are **not required for the first MVP**.

SkySentinel initially uses a simulated drone provider.

Example lifecycle:

```text
AVAILABLE
    ↓
TAKING_OFF
    ↓
IN_FLIGHT
    ↓
OBSERVING
    ↓
RETURNING
    ↓
LANDED
    ↓
AVAILABLE
```

Simulated telemetry can include:

```text
Drone ID
Status
Battery
Latitude
Longitude
Altitude
Speed
```

This allows the complete software workflow to be developed and demonstrated before integrating physical drone hardware.

---

## 🔌 Hardware-Independent Architecture

SkySentinel uses a drone abstraction layer.

```text
SkySentinel
     │
     ▼
Mission Service
     │
     ▼
DroneProvider
     │
 ┌───┴────────────────┐
 │                    │
 ▼                    ▼
Simulator        Future Supported
Provider          Drone Adapter
```

The MVP uses:

```text
SimulatedDroneProvider
```

Future supported drone or docking-station integrations can implement the same interface.

---

## 👥 User Roles

### Administrator

Manages:

- Users
- Sites
- Operational areas
- Panic buttons
- Drone bases
- Drones
- Permissions

### Control Room Operator

Responsible for:

- Monitoring incidents
- Acknowledging incidents
- Viewing the live map
- Dispatching response teams
- Creating authorized observation missions
- Monitoring active operations

### Drone Operator

Responsible for authorized drone observation missions and mission status.

### Response Officer

Receives dispatch information and updates response status.

---

## 🧱 Core Domain Model

The main entities include:

```text
SecurityCompany
UserAccount
OperationalArea
Site
PanicButton
Incident
DroneBase
Drone
Mission
Response
Evidence
AuditLog
```

Simplified relationship:

```text
SECURITY COMPANY
       │
       ├──── USERS
       │
       ├──── OPERATIONAL AREAS
       │          │
       │          ├──── SITES
       │          │       │
       │          │       ├── PANIC BUTTONS
       │          │       └── INCIDENTS
       │          │
       │          └──── DRONE BASES
       │                    │
       │                    └── DRONES
       │
       └─────────────────────────┐
                                 ▼
                             INCIDENT
                                 │
                   ┌─────────────┼─────────────┐
                   ▼             ▼             ▼
                MISSION       RESPONSE      EVIDENCE
                   │
                   ▼
                 DRONE
```

---

## 🛠️ Technology Stack

### Backend

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- Maven

### Frontend

- React
- HTML
- CSS
- JavaScript

### Database

- PostgreSQL

### Testing

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers where appropriate

### DevOps

- Docker
- Docker Compose
- Git
- CI/CD

---

## 🏗️ Application Architecture

```text
React Frontend
      │
      │ HTTPS / REST
      ▼
Spring Boot API
      │
      ▼
Controllers
      │
      ▼
Services
      │
      ├──── Incident Service
      ├──── Mission Service
      ├──── Drone Service
      └──── Response Service
      │
      ▼
Repositories
      │
      ▼
PostgreSQL
```

DTOs are used for API requests and responses instead of exposing database entities directly.

---

## 📡 Planned API

Examples:

```http
POST /api/auth/login

POST /api/panic-events

GET  /api/incidents
GET  /api/incidents/{id}
POST /api/incidents

PATCH /api/incidents/{id}/acknowledge
PATCH /api/incidents/{id}/resolve

GET  /api/operational-areas

GET  /api/sites

GET  /api/drone-bases
POST /api/drone-bases

GET  /api/drones
POST /api/drones

GET  /api/missions
POST /api/missions

PATCH /api/missions/{id}/start
PATCH /api/missions/{id}/complete

POST  /api/responses
PATCH /api/responses/{id}/dispatch
PATCH /api/responses/{id}/complete
```

---

## 🚨 Example Panic Event

```http
POST /api/panic-events
```

Example request:

```json
{
  "deviceCode": "PB-WH-002",
  "event": "PANIC"
}
```

SkySentinel will identify the registered panic button and protected site and create an incident.

The incident then appears in the Security Operations Centre.

---

## 🔐 Security

SkySentinel is being designed with:

- Authentication
- Role-based access control
- Secure password hashing
- Input validation
- Company/tenant data separation
- Audit logging
- Environment-based secret management
- HTTPS-ready configuration
- Protected evidence references
- Controlled access to sensitive location information

---

## 🧪 Testing Strategy

SkySentinel follows an acceptance-test-oriented development approach.

Example scenario:

```text
GIVEN

Warehouse B is registered
PB-WH-002 belongs to Warehouse B
Drone SS-001 is available
Response Team 03 is available

WHEN

PB-WH-002 sends a PANIC event

THEN

A HIGH-priority incident is created
and displayed to the control room.

WHEN

The operator acknowledges the incident

THEN

The incident becomes ACKNOWLEDGED.

WHEN

the operator explicitly creates
an observation mission

THEN

an eligible simulated drone can
be assigned to the mission.

AND

a security response team can
also be dispatched.
```

---

## 📁 Planned Repository Structure

```text
skysentinel-security/
│
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
├── frontend/
│   ├── src/
│   ├── package.json
│   └── Dockerfile
│
├── database/
│   ├── migrations/
│   └── seed/
│
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── database/
│   └── diagrams/
│
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

---

## 🗺️ Development Roadmap

### Phase 1 — Foundation

Set up:

- Git repository
- Spring Boot
- React
- PostgreSQL
- Docker
- Docker Compose

### Phase 2 — Authentication

Implement:

- Users
- Roles
- Login
- Spring Security
- Authorization

### Phase 3 — Security Infrastructure

Implement:

- Operational areas
- Sites
- Panic buttons
- Drone bases

### Phase 4 — Incident Management

Implement the first complete workflow:

```text
Panic
   ↓
Incident
   ↓
Database
   ↓
Control Room
   ↓
Acknowledge
```

### Phase 5 — Drone Management

Implement:

- Drone fleet
- Drone status
- Battery information
- Simulated drone provider
- Simulated telemetry

### Phase 6 — Missions

Implement:

- Mission creation
- Drone assignment
- Mission status
- Observation workflow

### Phase 7 — Response Teams

Implement:

- Response teams
- Dispatch
- On-scene status
- Completion

### Phase 8 — Security Operations Dashboard

Build the full SkySentinel dashboard.

### Phase 9 — Live Operations

Add:

- Live map
- Real-time incident updates
- Drone telemetry
- Response-team updates

### Phase 10 — Evidence & Reporting

Add:

- Evidence metadata
- Incident history
- Audit logs
- Reports

### Phase 11 — Deployment

Add:

- Docker production configuration
- CI/CD
- Cloud deployment
- Monitoring
- Backups

### Phase 12 — Real Drone Integration

Only after the software MVP has been validated:

- Evaluate supported enterprise drones
- Evaluate docking stations
- Integrate supported APIs/SDKs
- Integrate telemetry
- Integrate supported video workflows

---

## 🎬 Demo Scenario

The first SkySentinel demonstration will simulate:

```text
Industrial Area North
│
├── Warehouse A
├── Warehouse B
├── Warehouse C
│
└── 🚁 Shared Drone Base DB-001
    ├── SS-001
    └── SS-002

High-Risk Facility D
│
└── 🚁 Dedicated Drone Base DB-002
    └── SS-003

Site E
└── 🚓 Conventional response only
```

During the demo:

```text
Warehouse B Panic Button
          ↓
SkySentinel
          ↓
Incident Created
          ↓
Operator Acknowledges
          ↓
     ┌────┴────┐
     ▼         ▼
🚓 Response   🚁 Observation
   Team          Mission
     │              │
     └──────┬───────┘
            ▼
      Incident Resolved
            ↓
      Report + History
```

---

## 🚧 Project Status

**Status: In Development**

Current focus:

```text
Foundation
   ↓
Database
   ↓
Incident Management
   ↓
Drone Simulation
   ↓
Operations Dashboard
```

The first version uses simulated drones and demo security sites.

Physical drone integration is planned for a later stage after the software workflow has been validated.

---

## ⚠️ Important Note

SkySentinel is currently a **software development and demonstration project**.

Any future real-world drone deployment would need to account for the selected aircraft, communications, operating environment, airspace, privacy, security procedures, and applicable regulatory/operational requirements.

Demo operational areas and distances should therefore not be interpreted as approved real-world flight coverage.

---

## 🚀 Vision

SkySentinel's long-term vision is to give security companies a modern platform that combines:

**Human security teams + incident management + real-time information + optional drone observation**

into one operational environment.

> **SkySentinel Security — See sooner. Respond smarter.**