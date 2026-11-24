package barangaydamagereporter.service;

import barangaydamagereporter.model.DamageReport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * FILE HANDLING: saves and loads reports using a plain text file.
 * One report = one line, fields separated by "|".
 */
public class FileManager {

    private final String filePath;

    public FileManager(String filePath) {
        this.filePath = filePath;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean fileExists() {
        return new File(filePath).exists();
    }

    /** Loads every report from the file into an ArrayList. */
    public ArrayList<DamageReport> loadReports() {
        ArrayList<DamageReport> loaded = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {

            String line;
            // CONTROL STRUCTURE: while loop - read until there are no more lines
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    loaded.add(DamageReport.fromFileString(line));
                } catch (IllegalArgumentException e) {
                    // A damaged line is skipped instead of crashing the program
                    System.err.println("Skipped invalid line in file: " + e.getMessage());
                }
            }

        } catch (FileNotFoundException e) {
            // EXCEPTION HANDLING: first run, the file does not exist yet
            System.out.println("Data file not found (" + filePath + "). Starting with no reports.");
        } catch (IOException e) {
            // EXCEPTION HANDLING: any other reading problem
            throw new IllegalStateException("Could not read the data file: " + e.getMessage(), e);
        }
        return loaded;
    }

    /** Writes any text to a file (used by "Generate Report"). */
    public void writeText(String path, String text) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            writer.write(text);
        }
    }

    /** Saves the whole report list (overwrites the old file). */
    public void saveReports(ArrayList<DamageReport> reports) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8))) {
            for (DamageReport report : reports) { // enhanced for loop
                writer.write(report.toFileString());
                writer.newLine();
            }
        }
    }
}
