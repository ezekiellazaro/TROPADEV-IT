-- BarangayFixTracker database (SQLite).
-- The program creates this table by itself on first run (see SqliteReportRepository.createTable()).
-- The data file "barangayfixtracker.db" is created in this folder. Open it with "DB Browser for SQLite".
CREATE TABLE IF NOT EXISTS reports (
    report_id      TEXT PRIMARY KEY,
    reporter_name  TEXT NOT NULL,
    contact_number TEXT NOT NULL,
    facility_name  TEXT NOT NULL,
    facility_type  TEXT NOT NULL,
    barangay       TEXT NOT NULL,
    damage_type    TEXT NOT NULL,
    description    TEXT NOT NULL,
    severity       TEXT NOT NULL,
    estimated_cost REAL NOT NULL,
    date_reported  TEXT NOT NULL,
    status         TEXT NOT NULL,
    photo_path     TEXT NOT NULL DEFAULT ''
);
