package barangaydamagereporter.model;

import barangaydamagereporter.service.ReportManager;

/**
 * OOP CONCEPT: INHERITANCE - Admin "is a" User (extends User).
 * The barangay administrator who manages every report.
 */
public class Admin extends User {

    public Admin(String username, String password, String name) {
        super(username, password, name);
    }

    // OOP CONCEPT: POLYMORPHISM - same method name as Reporter, different result
    @Override
    public String displayRole() {
        return "Barangay Administrator";
    }

    @Override
    public boolean canManageReports() {
        return true;
    }

    @Override
    public String getDashboardTitle() {
        return "Admin Dashboard";
    }

    @Override
    public String getDashboardSubtitle() {
        return "Barangay Facility Damage Reporting and Monitoring System";
    }

    @Override
    public String getWelcomeMessage() {
        return "Welcome back, " + getName() + " (Admin)";
    }

    // ---- Admin-only behaviour ----
    public void updateReport(ReportManager manager, DamageReport report) {
        manager.updateReport(report);
    }

    public void deleteReport(ReportManager manager, String reportId) {
        manager.deleteReport(reportId);
    }

    /** Approving a report moves it from REPORTED to UNDER REVIEW. */
    public void approveReport(ReportManager manager, String reportId) {
        manager.changeStatus(reportId, "UNDER REVIEW");
    }

    public void updateStatus(ReportManager manager, String reportId, String newStatus) {
        manager.changeStatus(reportId, newStatus);
    }
}
