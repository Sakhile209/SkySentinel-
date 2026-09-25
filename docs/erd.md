# Planned domain relationships — not implemented in Phase 1

```mermaid
erDiagram
  SecurityCompany ||--o{ UserAccount : owns
  SecurityCompany ||--o{ OperationalArea : owns
  SecurityCompany ||--o{ Site : owns
  OperationalArea ||--o{ Site : groups
  OperationalArea ||--o{ DroneBase : groups
  DroneBase ||--o{ Drone : contains
  Site ||--o{ PanicButton : contains
  Site ||--o{ Incident : receives
  Incident ||--o{ Mission : has
  Incident ||--o{ Response : has
  Incident ||--o{ Evidence : has
  Drone ||--o{ Mission : assigned
  UserAccount ||--o{ Mission : authorizes
  SecurityCompany ||--o{ AuditLog : owns
```

This conceptual diagram preserves the agreed model; it is not a migration or a
complete foreign-key design. Response-team membership, base/site eligibility,
tenant constraints and audit actor links will be specified in their phases.
Phase 1 creates only Flyway schema history.
