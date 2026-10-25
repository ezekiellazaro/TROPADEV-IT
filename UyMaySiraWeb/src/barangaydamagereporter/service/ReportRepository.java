package barangaydamagereporter.service;

import barangaydamagereporter.model.DamageReport;
import java.util.ArrayList;

/**
 * OOP CONCEPT: ABSTRACTION (interface)
 * Says WHAT the storage must do. HOW it is done is decided by the class that implements it:
 * SqliteReportRepository (database) or FileReportRepository (text file).
 * ReportManager only talks to this interface, so the storage can be changed without touching it.
 */
public interface ReportRepository {

    /** True when the database / file did not exist yet (so sample data may be added). */
    boolean isFirstRun();

    ArrayList<DamageReport> loadAll();

    void insert(DamageReport report);

    void update(DamageReport report);

    void delete(String reportId);

    /** Short text for the screen, e.g. "SQLite database (uymaysira.db)". */
    String describe();

    String lastSavedText();
}
