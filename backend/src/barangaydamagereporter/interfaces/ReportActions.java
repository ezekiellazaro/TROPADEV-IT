package barangaydamagereporter.interfaces;

import barangaydamagereporter.model.DamageReport;

/**
 * OOP CONCEPT: ABSTRACTION (interface)
 * Lists WHAT a report system must do, without saying HOW.
 * ReportManager implements this interface and supplies the code.
 *
 * NOTE: the package is named "interfaces" because "interface" is a reserved
 * Java keyword and cannot be used as a package name.
 */
public interface ReportActions {

    void addReport(DamageReport report);

    void updateReport(DamageReport report);

    void deleteReport(String reportId);
}
