package barangaydamagereporter.model;

/**
 * One damage report. This is the main data object of the system.
 */
public class DamageReport {

    // OOP CONCEPT: ENCAPSULATION - every field is private
    private String reportId;
    private String reporterName;
    private String contactNumber;
    private Facility facility;          // composition (HAS-A)
    private String damageType;
    private String description;
    private String severity;
    private double estimatedCost;
    private String dateReported;
    private String status;
    private String photoPath = "";     // optional photo (relative path), "" = none

    public DamageReport(String reportId, String reporterName, String contactNumber,
                        Facility facility, String damageType, String description,
                        String severity, double estimatedCost,
                        String dateReported, String status) {
        this.reportId = reportId;
        this.reporterName = reporterName;
        this.contactNumber = contactNumber;
        this.facility = facility;
        this.damageType = damageType;
        this.description = description;
        this.severity = severity;
        setEstimatedCost(estimatedCost); // reuse the setter so its rule is applied
        this.dateReported = dateReported;
        this.status = status;
    }

    /**
     * OOP CONCEPT: POLYMORPHISM (constructor OVERLOADING) - same name, extra parameter.
     * This version also stores the optional photo path.
     */
    public DamageReport(String reportId, String reporterName, String contactNumber,
                        Facility facility, String damageType, String description,
                        String severity, double estimatedCost,
                        String dateReported, String status, String photoPath) {
        this(reportId, reporterName, contactNumber, facility, damageType, description,
             severity, estimatedCost, dateReported, status);
        this.photoPath = (photoPath == null) ? "" : photoPath;
    }

    // ---------------- Getters and setters ----------------
    public String getReportId() { return reportId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public Facility getFacility() { return facility; }
    public void setFacility(Facility facility) { this.facility = facility; }
    public String getDamageType() { return damageType; }
    public void setDamageType(String damageType) { this.damageType = damageType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public double getEstimatedCost() { return estimatedCost; }
    public String getDateReported() { return dateReported; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = (photoPath == null) ? "" : photoPath; }
    public boolean hasPhoto() { return !photoPath.isEmpty(); }

    // Shortcuts so other classes do not need report.getFacility().getName()
    public String getFacilityName() { return facility.getName(); }
    public String getFacilityType() { return facility.getType(); }
    public String getBarangay() { return facility.getBarangay(); }

    /** ENCAPSULATION: the setter protects the object from invalid data. */
    public void setEstimatedCost(double estimatedCost) {
        if (estimatedCost < 0) {
            throw new IllegalArgumentException("Estimated cost cannot be negative.");
        }
        this.estimatedCost = estimatedCost;
    }

    /**
     * PRIORITY SYSTEM: priority is worked out automatically from severity.
     * CONTROL STRUCTURE: switch statement.
     */
    public String getPriority() {
        String priority;
        switch (severity) {
            case "CRITICAL":
                priority = "URGENT";
                break;
            case "HIGH":
                priority = "HIGH PRIORITY";
                break;
            case "MODERATE":
                priority = "NORMAL";
                break;
            default:
                priority = "LOW PRIORITY";
        }
        return priority;
    }

    /** Multi-line text shown by the VIEW button. */
    public String getDetails() {
        return "Report ID:       " + reportId + "\n"
             + "Reporter:        " + reporterName + "\n"
             + "Contact Number:  " + contactNumber + "\n"
             + "Barangay:        " + getBarangay() + "\n"
             + "Facility:        " + getFacilityName() + "\n"
             + "Facility Type:   " + getFacilityType() + "\n"
             + "Damage Type:     " + damageType + "\n"
             + "Severity:        " + severity + "\n"
             + "Priority:        " + getPriority() + "\n"
             + String.format("Estimated Cost:  PHP %,.2f%n", estimatedCost)
             + "Date Reported:   " + dateReported + "\n"
             + "Status:          " + status + "\n\n"
             + "Description:\n" + description;
    }

    // ---------------- File conversion ----------------
    /** Turns this report into ONE line of text for damage_reports.txt. */
    public String toFileString() {
        return clean(reportId) + "|" + clean(reporterName) + "|" + clean(contactNumber) + "|"
             + clean(getFacilityName()) + "|" + clean(getFacilityType()) + "|" + clean(getBarangay()) + "|"
             + clean(damageType) + "|" + clean(description) + "|" + clean(severity) + "|"
             + estimatedCost + "|" + clean(dateReported) + "|" + clean(status) + "|" + clean(photoPath);
    }

    /** Rebuilds a report from one line of the file. */
    public static DamageReport fromFileString(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length != 12 && p.length != 13) { // older files have no photo column
            throw new IllegalArgumentException("Expected 12 or 13 fields but found " + p.length);
        }
        double cost;
        try {
            cost = Double.parseDouble(p[9]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid cost value in file: " + p[9]);
        }
        Facility facility = new Facility(p[3], p[4], p[5]);
        String photo = (p.length == 13) ? p[12] : "";
        return new DamageReport(p[0], p[1], p[2], facility, p[6], p[7], p[8], cost, p[10], p[11], photo);
    }

    /** The file uses "|" as separator, so remove it (and line breaks) from user text. */
    private static String clean(String text) {
        return text.replace("|", "/").replace("\r", " ").replace("\n", " ");
    }
}
