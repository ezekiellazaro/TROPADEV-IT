package barangaydamagereporter.web;

import barangaydamagereporter.service.FileManager;
import barangaydamagereporter.service.FileReportRepository;
import barangaydamagereporter.service.LoginManager;
import barangaydamagereporter.service.ReportManager;
import barangaydamagereporter.service.ReportRepository;
import barangaydamagereporter.service.SqliteReportRepository;
import java.io.IOException;

/**
 * START of the website. Opens the server (default port 8080).
 * Storage: SQLite database if the driver jar is in "lib", otherwise the text file.
 */
public class Main {

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port \"" + args[0] + "\", using 8080.");
            }
        }
        try {
            ReportManager manager = new ReportManager(chooseStorage());
            WebServer server = null;
            for (int attempt = 0; attempt < 10 && server == null; attempt++) {   // port busy? try the next one
                try {
                    server = new WebServer(port + attempt, manager, new LoginManager());
                    port = port + attempt;
                } catch (java.net.BindException e) {
                    System.out.println("[!] Port " + (port + attempt) + " is already in use (is another copy still running?). Trying " + (port + attempt + 1) + "...");
                }
            }
            if (server == null) {
                System.err.println("Could not find a free port. Close the other copy of the program and try again.");
                System.exit(1);
            }
            server.start();
            System.out.println("Uy May Sira! is running.  Storage: " + manager.getStorageDescription());
            System.out.println("Open your browser at:  http://localhost:" + port);
            System.out.println("(press Ctrl+C to stop)");
        } catch (IOException | IllegalStateException e) {
            System.err.println("Could not start the server: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Polymorphism / abstraction: both choices are used through the ReportRepository interface. */
    private static ReportRepository chooseStorage() {
        try {
            Class.forName("org.sqlite.JDBC");              // is the SQLite driver in lib/ ?
            return new SqliteReportRepository("uymaysira.db");
        } catch (ClassNotFoundException | LinkageError e) {
            System.out.println("[!] SQLite driver not found in the lib folder. Using the text file instead.");
            System.out.println("    See README.txt to add the driver and use the database.");
        } catch (IllegalStateException e) {
            System.out.println("[!] Could not open the database: " + e.getMessage());
            System.out.println("    Using the text file instead.");
        }
        return new FileReportRepository(new FileManager("damage_reports.txt"));
    }
}
