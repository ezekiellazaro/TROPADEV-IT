BARANGAYFIXTRACKER  -  WEBSITE VERSION (Java back end + web page + database)

WHAT IT IS
A website that runs on your computer (or a school server). The page is HTML/CSS/JavaScript in the "frontend" folder.
The back end is plain Java (no framework): it uses the HTTP server that comes with the JDK (com.sun.net.httpserver).
The same OOP classes are used: User > Reporter / Admin, DamageReport, Facility, ReportManager, ValidationUtil...

PROJECT STRUCTURE
  frontend/   the website the user sees: index.html, app.js, style.css, images
  backend/    the Java server: src/ (OOP classes, web server, REST API) and lib/ (SQLite driver .jar)
  database/   schema.sql (table design), barangayfixtracker.db (created automatically), photos/ (uploads)
  run.bat / run.sh   compile and start everything

HOW TO RUN
1. Install a JDK (11 or newer is recommended).
2. Windows: double-click run.bat      Mac/Linux: ./run.sh
3. Open your browser at  http://localhost:8080
   (another port: java -cp "backend/out;backend/lib/*" barangaydamagereporter.web.Main 9000)

LOGINS
  Admin:    admin / admin123
  Reporter: resident / resident123

DATABASE (SQLite)  - one small extra file is needed
1. Download the SQLite JDBC driver (the .jar of "sqlite-jdbc" by xerial) from Maven Central, for example
   version 3.41.2.2:  https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.41.2.2/
2. Put the .jar inside the "backend/lib" folder, then run again.
3. The console should say:  Storage: SQLite database (barangayfixtracker.db)
   The database file barangayfixtracker.db is created in the "database" folder. Open it with "DB Browser for SQLite" to see the table "reports".
If the driver is missing, the program still works and saves to database/damage_reports.txt instead (the console tells you).
If you get an error about "slf4j", also put an slf4j-api .jar in "lib" (or use the version above). run.bat / run.sh download both jars automatically the first time..

HOW IT IS BUILT (for your defense)
  Browser (frontend/index.html, app.js, style.css)  --JSON over HTTP-->  WebServer.java  -->  ReportManager  -->  ReportRepository (interface)
                                                                                                          |-- SqliteReportRepository (JDBC, database)
                                                                                                          |-- FileReportRepository (text file)
  Abstraction: ReportRepository hides HOW data is stored.  Polymorphism: Admin/Reporter objects answer the same methods differently.
  Roles are checked on the SERVER (a Reporter cannot delete or change status even by sending a request by hand).

FEATURES
  Login by role - report form with photo upload (JPG/PNG, max 5 MB) - status flow REPORTED > UNDER REVIEW > IN PROGRESS > REPAIRED > CLOSED
  Admin dashboard (cards, charts, activity) - search/filter/pages - edit/delete/approve - user management - summary export
  Alarm siren in the admin's browser when a new report arrives (the page checks every 5 seconds). Browsers only play sound after you
  have clicked something on the page (logging in counts). Turn it on/off in Settings.

OTHER COMPUTERS: while the server runs, other devices on the same Wi-Fi can open  http://<this-computer-IP>:8080
  (allow Java through the firewall if asked).

LIMITS (honest list)
  - Passwords are plain text and user accounts are kept in memory (new accounts disappear when the server restarts).
  - No HTTPS, no password reset, no limit on login attempts: good for a school project, not for the real internet.
  - Photos are saved in "database/photos"; deleting a report deletes its photo.
