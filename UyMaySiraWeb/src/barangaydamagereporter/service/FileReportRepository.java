package barangaydamagereporter.service;

import barangaydamagereporter.model.DamageReport;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/** OOP: implements ReportRepository using the plain text file (used when no database driver is installed). */
public class FileReportRepository implements ReportRepository {

    private final FileManager fileManager;
    private final boolean firstRun;
    private ArrayList<DamageReport> cache = new ArrayList<>();

    public FileReportRepository(FileManager fileManager) {
        this.fileManager = fileManager;
        this.firstRun = !fileManager.fileExists();
    }

    @Override public boolean isFirstRun() { return firstRun; }

    @Override
    public ArrayList<DamageReport> loadAll() {
        cache = fileManager.loadReports();
        return new ArrayList<>(cache);
    }

    @Override
    public void insert(DamageReport report) {
        cache.add(report);
        saveAll();
    }

    @Override
    public void update(DamageReport report) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getReportId().equals(report.getReportId())) {
                cache.set(i, report);
                break;
            }
        }
        saveAll();
    }

    @Override
    public void delete(String reportId) {
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).getReportId().equals(reportId)) {
                cache.remove(i);
                break;
            }
        }
        saveAll();
    }

    @Override public String describe() { return "Text file (" + fileManager.getFilePath() + ")"; }

    @Override
    public String lastSavedText() {
        File file = new File(fileManager.getFilePath());
        if (!file.exists()) {
            return "Not saved yet";
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("MMM d, hh:mm a"));
    }

    private void saveAll() {
        try {
            fileManager.saveReports(cache);
        } catch (IOException e) {
            throw new IllegalStateException("Could not save reports to file: " + e.getMessage(), e);
        }
    }
}
