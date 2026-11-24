package barangaydamagereporter.model;

import barangaydamagereporter.service.ReportManager;

/**
 * OOP CONCEPT: INHERITANCE - Reporter "is a" User (extends User).
 * A barangay resident or staff member who submits damage reports.
 */
public class Reporter extends User {

    public Reporter(String username, String password, String name) {
        super(username, password, name); // calls the parent constructor
    }

    // OOP CONCEPT: POLYMORPHISM - method OVERRIDING (Reporter's own version)
    @Override
    public String displayRole() {
        return "Barangay Reporter";
    }

    @Override
    public boolean canManageReports() {
        return false; // reporters cannot edit, delete or change status
    }

    @Override
    public String getDashboardTitle() {
        return "Welcome, " + getName() + "!";
    }

    @Override
    public String getDashboardSubtitle() {
        return "Report damaged facilities and help make our barangay safer and stronger.";
    }

    @Override
    public String getWelcomeMessage() {
        return "Hello, " + getName() + " (Resident)";
    }

    /** Reporter-only behaviour. */
    public void submitReport(ReportManager manager, DamageReport report) {
        manager.addReport(report);
    }
}
