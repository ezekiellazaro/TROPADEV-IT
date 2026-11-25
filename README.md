<p align="center">
  <img src="banner.svg" alt="Barangay FixTrack - Team TROPADEV" width="100%">
</p>

# 🛠️TROPADEV-IT: Barangay FixTrack🚧🛠️

**OOP 101 - Object-Oriented Programming** | BSIT-2106 | Team 6 (TROPADEV-IT)
Batangas State University, The National Engineering University, Alangilan Campus

Barangay FixTrack🔎 is a Java-based web system where residents report damaged barangay facilities and barangay officials review, update and monitor the reports until they are resolved.

## OOP Concepts Demonstrated

| OOP Concept | How it was demonstrated |
|---|---|
| **Encapsulation** | The attributes of `User`, `DamageReport` and `Facility` are declared `private` and accessed only through getters and setters. `ValidationUtil` checks inputs before they are stored, so invalid or missing data cannot reach the report. |
| **Inheritance** | `Reporter` and `Admin` extend the parent class `User`. They reuse its shared attributes and methods (such as `login()` and `logout()`) and add features of their own, calling the parent constructor with `super()`. |
| **Polymorphism** | `Reporter` and `Admin` override the `User` methods that differ by role. A `User` reference can hold either one, and the correct version runs at runtime, so the main menu shows resident or official options depending on who logged in. |
| **Abstraction** | The `ReportActions` interface defines the report operations without saying how they work. The `ReportRepository` interface hides how reports are stored, so `ReportManager` works the same whether data is saved by `SqliteReportRepository` or `FileReportRepository`. |

## Team Members and Assigned Tasks

| Member | SR-Code | GitHub | Role | Assigned Tasks |
|---|---|---|---|---|
| Catapang, Hannah D. | 25-02798 | catapanghannah | Backend + Frontend | Damage Report Submission and Photo Upload. `Resident` and `Facility` classes, `submitReport()` and `viewReport()`, input validation, exception handling for invalid or missing inputs. |
| Lazaro, Ezekiel A. | 25-09214 | ezekiellazaro | Project Lead, Backend | Core system. `User` parent class (inheritance and polymorphism), `login()` and `logout()`, main menu and program flow. Manages this repository and merges the team's code. |
| Piañar, Alexandra A. | 25-06977 | Alexandra Pianar | Backend + Frontend | Barangay Official features. `Barangay Officials` class, `reviewReport()` and `updateStatus()`, Search and Filter, Dashboard summary of reports. |
| Roxas, Nycka M. | 25-00982 | Nycka-Roxas28 | Database, Testing, Docs | Data handling (arrays/data structures), SQLite storage, testing and debugging, documentation, slides and final presentation. |

## Project Structure

```
BarangayFixTrackerWeb/
├── frontend/   Website (index.html, app.js, style.css, images)
├── backend/
│   ├── src/    Java source code (model, service, util, interfaces, web)
│   └── lib/    SQLite driver .jar (downloaded automatically by run.bat / run.sh)
├── database/   schema.sql (table design); the .db file and photos are created on run
├── run.bat     Windows: compile and start
└── run.sh      Mac/Linux: compile and start
```

### Who owns which files

| Member | Files |
|---|---|
| Hannah | `model/Reporter.java`, `model/Facility.java`, `util/ValidationUtil.java`, report form and photo upload in `frontend/` |
| Ezekiel | `model/User.java`, `service/LoginManager.java`, `interfaces/ReportActions.java`, `web/Main.java`, `web/WebServer.java`, `web/AppPaths.java` |
| Alexandra | `model/Admin.java`, `model/ActivityEntry.java`, dashboard, search/filter and status update in `frontend/` |
| Nycka | `model/DamageReport.java`, `service/ReportManager.java`, `service/ReportRepository.java`, `service/SqliteReportRepository.java`, `service/FileReportRepository.java`, `service/FileManager.java`, `database/`, docs |

## How to Run

1. Install a JDK (11 or newer).
2. Windows: double-click `run.bat` | Mac/Linux: `./run.sh`
3. Open http://localhost:8080

Logins: Admin `admin` / `admin123`, Reporter `resident` / `resident123`

The first run downloads the SQLite driver (internet needed once). See `README.txt` for details.

## Git Workflow

1. `git pull origin main` before working.
2. Work on your own branch (e.g. `hannah-validation`).
3. Commit small changes with clear messages.
4. Open a Pull Request; Ezekiel reviews and merges.

## UML Class Diagram

```mermaid
flowchart TD

subgraph group_web["Web interface"]
  node_browser["Browser UI<br/>[app.js]"]
  node_server["HTTP server<br/>[WebServer.java]"]
  node_paths["App paths<br/>[AppPaths.java]"]
  node_json["JSON codec<br/>[Json.java]"]
  node_main["Application startup<br/>[Main.java]"]
end

subgraph group_identity["Users and access"]
  node_login["Login manager<br/>[LoginManager.java]"]
  node_user["User roles<br/>[User.java]"]
  node_admin["Administrator<br/>[Admin.java]"]
  node_reporter["Reporter<br/>[Reporter.java]"]
end

subgraph group_reports["Report domain"]
  node_validation["Input validation"]
  node_actions["Report actions<br/>[ReportActions.java]"]
  node_manager["Report manager<br/>[ReportManager.java]"]
  node_damage["Damage report<br/>[DamageReport.java]"]
  node_facility["Facility<br/>[Facility.java]"]
  node_activity["Activity entry<br/>[ActivityEntry.java]"]
end

subgraph group_storage["Persistence"]
  node_sqlite[("SQLite repository")]
  node_file_repo["File repository"]
  node_file_manager["Text file manager<br/>[FileManager.java]"]
  node_repository["Repository contract"]
end

node_resident(("Resident or official"))

node_resident -->|"uses"| node_browser
node_browser -.->|"sends requests"| node_server
node_main -->|"starts"| node_server
node_main -->|"creates"| node_manager
node_main -->|"creates"| node_login
node_main -->|"selects"| node_sqlite
node_main -->|"fallback selects"| node_file_repo
node_server -->|"authenticates"| node_login
node_server -->|"handles reports"| node_manager
node_server -->|"encodes responses"| node_json
node_server -->|"resolves paths"| node_paths
node_login -->|"creates"| node_admin
node_login -->|"creates"| node_reporter
node_admin -->|"extends"| node_user
node_reporter -->|"extends"| node_user
node_admin -->|"delegates updates"| node_manager
node_reporter -->|"submits reports"| node_manager
node_manager -->|"loads and saves"| node_repository
node_manager -->|"validates input"| node_validation
node_manager -->|"records activity"| node_activity
node_manager -->|"implements"| node_actions
node_damage -->|"contains"| node_facility
node_sqlite -->|"implements"| node_repository
node_file_repo -->|"implements"| node_repository
node_file_repo -->|"uses"| node_file_manager
node_file_manager -->|"loads and saves"| node_damage

classDef toneBlue fill:#dbeafe,stroke:#2563eb,stroke-width:1.5px,color:#172554
classDef toneAmber fill:#fef3c7,stroke:#d97706,stroke-width:1.5px,color:#78350f
classDef toneMint fill:#dcfce7,stroke:#16a34a,stroke-width:1.5px,color:#14532d
classDef toneRose fill:#ffe4e6,stroke:#e11d48,stroke-width:1.5px,color:#881337
classDef toneIndigo fill:#e0e7ff,stroke:#4f46e5,stroke-width:1.5px,color:#312e81
class node_browser,node_server,node_paths,node_json,node_main toneBlue
class node_login,node_user,node_admin,node_reporter toneAmber
class node_validation,node_actions,node_manager,node_damage,node_facility,node_activity toneMint
class node_sqlite,node_file_repo,node_file_manager,node_repository toneRose
class node_resident toneIndigo
```
