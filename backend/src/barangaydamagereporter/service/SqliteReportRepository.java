package barangaydamagereporter.service;

import barangaydamagereporter.model.DamageReport;
import barangaydamagereporter.model.Facility;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * DATABASE STORAGE (JDBC + SQLite).
 * OOP: implements ReportRepository. Needs the sqlite-jdbc driver .jar inside the "lib" folder.
 */
public class SqliteReportRepository implements ReportRepository {

    private final File dbFile;
    private final String url;
    private final boolean firstRun;

    public SqliteReportRepository(String path) {
        this.dbFile = new File(path);
        this.firstRun = !dbFile.exists();
        this.url = "jdbc:sqlite:" + path;
        createTable();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS reports ("
                + "report_id TEXT PRIMARY KEY, reporter_name TEXT NOT NULL, contact_number TEXT NOT NULL, "
                + "facility_name TEXT NOT NULL, facility_type TEXT NOT NULL, barangay TEXT NOT NULL, "
                + "damage_type TEXT NOT NULL, description TEXT NOT NULL, severity TEXT NOT NULL, "
                + "estimated_cost REAL NOT NULL, date_reported TEXT NOT NULL, status TEXT NOT NULL, "
                + "photo_path TEXT NOT NULL DEFAULT '')";
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create the database table: " + e.getMessage(), e);
        }
    }

    @Override public boolean isFirstRun() { return firstRun; }

    @Override
    public ArrayList<DamageReport> loadAll() {
        ArrayList<DamageReport> list = new ArrayList<>();
        try (Connection c = connect(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM reports ORDER BY report_id")) {
            while (rs.next()) {
                Facility facility = new Facility(rs.getString("facility_name"),
                        rs.getString("facility_type"), rs.getString("barangay"));
                list.add(new DamageReport(rs.getString("report_id"), rs.getString("reporter_name"),
                        rs.getString("contact_number"), facility, rs.getString("damage_type"),
                        rs.getString("description"), rs.getString("severity"),
                        rs.getDouble("estimated_cost"), rs.getString("date_reported"),
                        rs.getString("status"), rs.getString("photo_path")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not read reports from the database: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void insert(DamageReport r) {
        String sql = "INSERT INTO reports (report_id, reporter_name, contact_number, facility_name, "
                + "facility_type, barangay, damage_type, description, severity, estimated_cost, "
                + "date_reported, status, photo_path) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getReportId());
            fill(ps, r, 2);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the report to the database: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(DamageReport r) {
        String sql = "UPDATE reports SET reporter_name=?, contact_number=?, facility_name=?, facility_type=?, "
                + "barangay=?, damage_type=?, description=?, severity=?, estimated_cost=?, date_reported=?, "
                + "status=?, photo_path=? WHERE report_id=?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            fill(ps, r, 1);
            ps.setString(13, r.getReportId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update the report in the database: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String reportId) {
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement("DELETE FROM reports WHERE report_id=?")) {
            ps.setString(1, reportId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not delete the report from the database: " + e.getMessage(), e);
        }
    }

    /** Sets the 12 columns after report_id, starting at parameter number "start". */
    private void fill(PreparedStatement ps, DamageReport r, int start) throws SQLException {
        int i = start;
        ps.setString(i++, r.getReporterName());
        ps.setString(i++, r.getContactNumber());
        ps.setString(i++, r.getFacilityName());
        ps.setString(i++, r.getFacilityType());
        ps.setString(i++, r.getBarangay());
        ps.setString(i++, r.getDamageType());
        ps.setString(i++, r.getDescription());
        ps.setString(i++, r.getSeverity());
        ps.setDouble(i++, r.getEstimatedCost());
        ps.setString(i++, r.getDateReported());
        ps.setString(i++, r.getStatus());
        ps.setString(i++, r.getPhotoPath());
    }

    @Override public String describe() { return "SQLite database (" + dbFile.getName() + ")"; }

    @Override
    public String lastSavedText() {
        if (!dbFile.exists()) {
            return "Not saved yet";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(dbFile.lastModified()), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM d, hh:mm a"));
    }
}
