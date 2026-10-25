package barangaydamagereporter.service;

import barangaydamagereporter.interfaces.ReportActions;
import barangaydamagereporter.model.ActivityEntry;
import barangaydamagereporter.model.DamageReport;
import barangaydamagereporter.model.Facility;
import barangaydamagereporter.util.ValidationUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

/**
 * Business logic for reports.
 * OOP CONCEPT: ABSTRACTION - implements the ReportActions interface.
 * COLLECTIONS: stores every report in an ArrayList<DamageReport>.
 */
public class ReportManager implements ReportActions {

    public static final String[] SEARCH_CRITERIA = {
        "All Fields", "Report ID", "Facility Name", "Barangay", "Facility Type", "Severity", "Status"
    };

    // ENCAPSULATION: private lists; other classes get COPIES through getAllReports()
    private ArrayList<DamageReport> reports;
    private final ArrayList<ActivityEntry> activities = new ArrayList<>();
    private final ReportRepository repository;   // database OR text file (abstraction)

    public ReportManager(ReportRepository repository) {
        this.repository = repository;
        boolean firstRun = repository.isFirstRun();
        reports = repository.loadAll();
        if (firstRun && reports.isEmpty()) {           // very first start: add the sample reports
            loadSampleData();
            for (DamageReport sample : reports) {
                repository.insert(sample);
            }
        }
        seedActivities();
    }

    // ------------- The three methods required by ReportActions -------------

    @Override
    public void addReport(DamageReport report) {
        if (report == null) {
            throw new IllegalArgumentException("Report cannot be null.");
        }
        if (findById(report.getReportId()) != null) {        // no duplicate IDs
            throw new IllegalArgumentException("Report ID " + report.getReportId() + " already exists.");
        }
        repository.insert(report);                             // save first (database or file)
        reports.add(report);                                   // ArrayList.add()
        logActivity("ADD", "New report submitted",
                report.getReportId() + " - " + report.getFacilityName(), report.getReporterName());
    }

    @Override
    public void updateReport(DamageReport updated) {
        for (int i = 0; i < reports.size(); i++) {             // for loop
            if (reports.get(i).getReportId().equals(updated.getReportId())) { // ArrayList.get()
                repository.update(updated);
                reports.set(i, updated);                       // ArrayList.set()
                logActivity("EDIT", "Report edited",
                        updated.getReportId() + " - " + updated.getFacilityName(), updated.getReporterName());
                return;
            }
        }
        throw new IllegalArgumentException("Report " + updated.getReportId() + " was not found.");
    }

    @Override
    public void deleteReport(String reportId) {
        for (int i = 0; i < reports.size(); i++) {
            if (reports.get(i).getReportId().equals(reportId)) {
                repository.delete(reportId);
                DamageReport removed = reports.remove(i);      // ArrayList.remove()
                logActivity("DELETE", "Report deleted",
                        removed.getReportId() + " - " + removed.getFacilityName(), removed.getReporterName());
                return;
            }
        }
        throw new IllegalArgumentException("Report " + reportId + " was not found.");
    }

    // ------------- Finding, searching, filtering -------------

    public DamageReport findById(String reportId) {
        for (DamageReport report : reports) {                  // enhanced for loop
            if (report.getReportId().equalsIgnoreCase(reportId)) {
                return report;
            }
        }
        return null;
    }

    public ArrayList<DamageReport> getAllReports() {
        return new ArrayList<>(reports);
    }

    /** Reports submitted by one person (used by the Reporter's "My Reports"). */
    public ArrayList<DamageReport> getReportsByReporter(String reporterName) {
        ArrayList<DamageReport> mine = new ArrayList<>();
        for (DamageReport report : reports) {
            if (report.getReporterName().equalsIgnoreCase(reporterName)) {
                mine.add(report);
            }
        }
        return mine;
    }

    public int getTotalReports() {
        return reports.size();                                 // ArrayList.size()
    }

    /** Search by one criteria (see SEARCH_CRITERIA). */
    public ArrayList<DamageReport> search(String criteria, String keyword) {
        ArrayList<DamageReport> results = new ArrayList<>();
        String key = keyword.trim().toLowerCase();

        for (DamageReport report : reports) {
            String target;
            switch (criteria) {                                // switch statement
                case "All Fields":
                    target = report.getReportId() + " " + report.getFacilityName() + " "
                           + report.getBarangay() + " " + report.getFacilityType() + " "
                           + report.getReporterName();
                    break;
                case "Report ID":      target = report.getReportId();     break;
                case "Facility Name":  target = report.getFacilityName(); break;
                case "Barangay":       target = report.getBarangay();     break;
                case "Facility Type":  target = report.getFacilityType(); break;
                case "Severity":       target = report.getSeverity();     break;
                case "Status":         target = report.getStatus();       break;
                default:
                    throw new IllegalArgumentException("Unknown search criteria: " + criteria);
            }
            if (target.toLowerCase().contains(key)) {
                results.add(report);
            }
        }
        return results;
    }

    /** Filter: "All", "Low", "Moderate", "High", "Critical". */
    public ArrayList<DamageReport> filterBySeverity(ArrayList<DamageReport> list, String filter) {
        if (filter == null || filter.equalsIgnoreCase("All")) {
            return list;
        }
        ArrayList<DamageReport> filtered = new ArrayList<>();
        for (DamageReport report : list) {
            if (report.getSeverity().equalsIgnoreCase(filter)) {
                filtered.add(report);
            }
        }
        return filtered;
    }

    /** Filter: "All Status" or one of the five statuses. */
    public ArrayList<DamageReport> filterByStatus(ArrayList<DamageReport> list, String filter) {
        if (filter == null || filter.equalsIgnoreCase("All Status")) {
            return list;
        }
        ArrayList<DamageReport> filtered = new ArrayList<>();
        for (DamageReport report : list) {
            if (report.getStatus().equalsIgnoreCase(filter)) {
                filtered.add(report);
            }
        }
        return filtered;
    }

    // ------------- Statistics for the dashboard -------------

    public int countBySeverity(String severity) {
        int count = 0;
        for (DamageReport report : reports) {
            if (report.getSeverity().equals(severity)) {
                count++;
            }
        }
        return count;
    }

    public int countByStatus(String status) {
        int count = 0;
        for (DamageReport report : reports) {
            if (report.getStatus().equals(status)) {
                count++;
            }
        }
        return count;
    }

    public int countByFacilityType(String type) {
        int count = 0;
        for (DamageReport report : reports) {
            if (report.getFacilityType().equals(type)) {
                count++;
            }
        }
        return count;
    }

    /** Counts reports from the last 7 days. Pass null for "any severity" / "any status". */
    public int countNewThisWeek(String severity, String status) {
        int count = 0;
        LocalDate weekAgo = LocalDate.now().minusDays(7);
        for (DamageReport report : reports) {
            boolean severityOk = (severity == null) || report.getSeverity().equals(severity);
            boolean statusOk = (status == null) || report.getStatus().equals(status);
            if (!severityOk || !statusOk) {
                continue;
            }
            try {
                if (!LocalDate.parse(report.getDateReported()).isBefore(weekAgo)) {
                    count++;
                }
            } catch (DateTimeParseException e) {
                System.err.println("Bad date in " + report.getReportId() + ": " + report.getDateReported());
            }
        }
        return count;
    }

    public double getTotalEstimatedCost() {
        double total = 0;
        for (DamageReport report : reports) {
            total += report.getEstimatedCost();
        }
        return total;
    }

    // ------------- Status workflow -------------

    /** Returns the only status allowed after "current", or null if it is already CLOSED. */
    public String getNextStatus(String current) {
        String[] flow = ValidationUtil.STATUSES;
        for (int i = 0; i < flow.length - 1; i++) {
            if (flow[i].equals(current)) {
                return flow[i + 1];
            }
        }
        return null;
    }

    /** REPORTED -> UNDER REVIEW -> IN PROGRESS -> REPAIRED -> CLOSED (one step at a time). */
    public void changeStatus(String reportId, String newStatus) {
        DamageReport report = findById(reportId);
        if (report == null) {
            throw new IllegalArgumentException("Report " + reportId + " was not found.");
        }
        ValidationUtil.validateInList(newStatus, ValidationUtil.STATUSES, "Status");

        String allowed = getNextStatus(report.getStatus());
        if (allowed == null) {
            throw new IllegalArgumentException(
                "This report is already CLOSED. Its status can no longer be changed.");
        }
        if (!allowed.equals(newStatus)) {
            throw new IllegalArgumentException("Invalid status change: " + report.getStatus()
                + " -> " + newStatus + ".\nThe only allowed next status is: " + allowed + ".");
        }
        report.setStatus(newStatus);
        repository.update(report);
        logActivity("STATUS", "Status updated",
                report.getReportId() + " - " + report.getFacilityName() + " -> " + newStatus,
                report.getReporterName());
    }

    // ------------- ID generation -------------

    /** RPT-001, RPT-002, ... : highest existing number + 1. */
    public String generateNextId() {
        int max = 0;
        for (DamageReport report : reports) {
            String id = report.getReportId();
            if (id.startsWith("RPT-")) {
                try {
                    int number = Integer.parseInt(id.substring(4));
                    if (number > max) {
                        max = number;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Ignoring non-standard report ID: " + id);
                }
            }
        }
        return String.format("RPT-%03d", max + 1);
    }

    // ------------- Recent activity -------------

    /** Newest first. Pass an owner name to see only that person's activity, or null for all. */
    public ArrayList<ActivityEntry> getRecentActivities(int limit, String owner) {
        ArrayList<ActivityEntry> result = new ArrayList<>();
        for (int i = activities.size() - 1; i >= 0 && result.size() < limit; i--) {
            ActivityEntry entry = activities.get(i);
            if (owner == null || entry.getOwner().equalsIgnoreCase(owner)) {
                result.add(entry);
            }
        }
        return result;
    }

    private void logActivity(String kind, String title, String detail, String owner) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
        activities.add(new ActivityEntry(kind, title, detail, time, owner));
    }

    /** At start-up the log is filled from the newest saved reports. */
    private void seedActivities() {
        int start = Math.max(0, reports.size() - 5);
        for (int i = start; i < reports.size(); i++) {
            DamageReport r = reports.get(i);
            activities.add(new ActivityEntry("ADD", "New report submitted",
                    r.getReportId() + " - " + r.getFacilityName(), r.getDateReported(), r.getReporterName()));
        }
    }

    // ------------- Storage info and summary -------------

    public String getStorageDescription() {
        return repository.describe();
    }

    public String getLastSavedText() {
        return repository.lastSavedText();
    }

    /** "Generate Report": a text summary of all reports. */
    public String buildSummary() {
        StringBuilder text = new StringBuilder();
        text.append("UY MAY SIRA! - DAMAGE REPORT SUMMARY\n");
        text.append("Generated: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMM d, yyyy hh:mm a"))).append("\n\n");
        text.append("TOTAL DAMAGE REPORTS: ").append(reports.size()).append("\n");
        String[] severities = ValidationUtil.SEVERITIES;
        for (int i = severities.length - 1; i >= 0; i--) {
            text.append(String.format("%-10s: %d%n", severities[i], countBySeverity(severities[i])));
        }
        text.append("\nBY STATUS\n");
        for (String status : ValidationUtil.STATUSES) {
            text.append(String.format("%-13s: %d%n", status, countByStatus(status)));
        }
        text.append(String.format("%nTOTAL ESTIMATED COST: PHP %,.2f%n%nREPORTS%n", getTotalEstimatedCost()));
        for (DamageReport r : reports) {
            text.append(String.format("%s | %s | %s | %s | %s | PHP %,.2f | %s%n", r.getReportId(),
                    r.getFacilityName(), r.getBarangay(), r.getSeverity(), r.getPriority(),
                    r.getEstimatedCost(), r.getStatus()));
        }
        return text.toString();
    }

    private void loadSampleData() {
        LocalDate today = LocalDate.now();
        reports.add(new DamageReport("RPT-001", "Juan Dela Cruz", "09171234567",
            new Facility("Barangay Hall", "Barangay Hall", "Poblacion"),
            "Flood Damage", "Flood water entered the hall after heavy rain and damaged the ground floor.",
            "HIGH", 25000, today.minusDays(9).toString(), "UNDER REVIEW"));

        reports.add(new DamageReport("RPT-002", "Juan Dela Cruz", "09171234567",
            new Facility("Barangay Road (Purok 3)", "Barangay Road", "Poblacion"),
            "Road Damage", "Large potholes and cracks appeared along the road after heavy rain.",
            "CRITICAL", 75000, today.minusDays(7).toString(), "REPORTED"));

        reports.add(new DamageReport("RPT-003", "Juan Dela Cruz", "09171234567",
            new Facility("Health Center", "Health Center", "Poblacion"),
            "Water Damage", "Roof leak is dripping water into the medicine storage room.",
            "MODERATE", 15000, today.minusDays(6).toString(), "IN PROGRESS"));

        reports.add(new DamageReport("RPT-004", "Ana Cruz", "09201234567",
            new Facility("Public School", "Public School", "San Jose"),
            "Structural Damage", "Classroom wall developed large cracks after the earthquake.",
            "HIGH", 60000, today.minusDays(4).toString(), "REPAIRED"));

        reports.add(new DamageReport("RPT-005", "Jose Lim", "09211234567",
            new Facility("Drainage System", "Drainage System", "San Jose"),
            "Flood Damage", "Clogged and broken drainage causes flooding on the street.",
            "MODERATE", 20000, today.minusDays(2).toString(), "UNDER REVIEW"));
    }
}
