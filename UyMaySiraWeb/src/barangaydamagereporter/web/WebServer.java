package barangaydamagereporter.web;

import barangaydamagereporter.model.ActivityEntry;
import barangaydamagereporter.model.Admin;
import barangaydamagereporter.model.DamageReport;
import barangaydamagereporter.model.Facility;
import barangaydamagereporter.model.Reporter;
import barangaydamagereporter.model.User;
import barangaydamagereporter.service.LoginManager;
import barangaydamagereporter.service.ReportManager;
import barangaydamagereporter.util.ValidationUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * The website's back end, written with the HTTP server that is already inside Java
 * (com.sun.net.httpserver). No framework. The web page (folder "web") talks to it with
 * JSON requests such as GET /api/reports. All rules (validation, roles, status flow)
 * are done by the same OOP classes as the desktop version.
 */
public class WebServer {

    /** An error with an HTTP status code (401 = not logged in, 403 = not allowed, 404 = not found). */
    private static class HttpError extends RuntimeException {
        private final int status;

        HttpError(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    private static final String[] FACILITY_GROUPS = {
        "Barangay Hall", "Barangay Road", "Health Center", "Public School", "Drainage System"
    };
    private static final String[] FACILITY_LABELS = {
        "Barangay Hall", "Road", "Health Center", "School", "Drainage", "Other"
    };

    private final ReportManager manager;
    private final LoginManager logins;
    private final HttpServer server;
    private final Map<String, User> sessions = new ConcurrentHashMap<>();   // cookie token -> logged-in user
    private final Object lock = new Object();                                // one request changes data at a time
    private final SecureRandom random = new SecureRandom();
    private final File webDir = new File("web");
    private final File photosDir = new File("photos");

    public WebServer(int port, ReportManager manager, LoginManager logins) throws IOException {
        this.manager = manager;
        this.logins = logins;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/photos/", this::handlePhoto);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(8));
    }

    public void start() {
        if (!photosDir.exists() && !photosDir.mkdirs()) {
            System.err.println("Could not create the photos folder.");
        }
        server.start();
    }

    // ===================== API entry point =====================

    private void handleApi(HttpExchange ex) throws IOException {
        try {
            synchronized (lock) {
                String method = ex.getRequestMethod();
                String path = ex.getRequestURI().getPath();
                if (path.equals("/api/export") && method.equals("GET")) {
                    requireAdmin(requireUser(ex));
                    ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=report_summary.txt");
                    send(ex, 200, "text/plain; charset=utf-8", manager.buildSummary().getBytes(StandardCharsets.UTF_8));
                    return;
                }
                sendJson(ex, 200, route(method, path, query(ex.getRequestURI()), ex));
            }
        } catch (HttpError e) {
            sendJson(ex, e.status, Json.obj("error", e.getMessage()));
        } catch (IllegalArgumentException e) {            // invalid input
            sendJson(ex, 400, Json.obj("error", e.getMessage()));
        } catch (IllegalStateException e) {               // database / file problem
            System.err.println("Storage error: " + e.getMessage());
            sendJson(ex, 500, Json.obj("error", e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            sendJson(ex, 500, Json.obj("error", "Something went wrong on the server."));
        }
    }

    private String route(String method, String path, Map<String, String> q, HttpExchange ex) throws IOException {
        if (path.equals("/api/login")) {
            return login(ex, body(ex));
        }
        if (path.equals("/api/logout")) {
            sessions.remove(token(ex));
            ex.getResponseHeaders().add("Set-Cookie", "session=; Path=/; Max-Age=0");
            return Json.obj("ok", true);
        }

        User user = requireUser(ex);       // everything below needs a logged-in user
        switch (path) {
            case "/api/me":
                return Json.obj("username", user.getUsername(), "name", user.getName(),
                        "role", user.displayRole(), "manage", user.canManageReports(),
                        "title", user.getDashboardTitle(), "subtitle", user.getDashboardSubtitle());
            case "/api/stats":
                return stats(user);
            case "/api/password":
                return changePassword(user, body(ex));
            case "/api/users":
                requireAdmin(user);
                if (method.equals("POST")) {
                    Map<String, String> b = body(ex);
                    logins.addReporter(get(b, "name"), get(b, "username"), get(b, "password"));
                }
                return listUsers();
            case "/api/reports":
                return method.equals("POST") ? createReport(user, body(ex)) : listReports(user, q);
            default:
                break;
        }

        if (path.startsWith("/api/reports/")) {
            String[] parts = path.substring("/api/reports/".length()).split("/");
            String id = parts[0];
            if (!id.matches("RPT-\\d+")) {
                throw new HttpError(404, "Report not found.");
            }
            if (parts.length == 2 && parts[1].equals("status") && method.equals("POST")) {
                findOwned(user, id);
                requireAdmin(user).updateStatus(manager, id, get(body(ex), "status"));
                return Json.obj("ok", true, "status", manager.findById(id).getStatus());
            }
            if (parts.length == 1) {
                DamageReport report = findOwned(user, id);
                switch (method) {
                    case "GET":
                        return reportJson(report);
                    case "PUT":
                        return updateReport(requireAdmin(user), report, body(ex));
                    case "DELETE":
                        requireAdmin(user).deleteReport(manager, id);
                        deletePhotoFile(report.getPhotoPath());
                        return Json.obj("ok", true);
                    default:
                        break;
                }
            }
        }
        throw new HttpError(404, "Not found.");
    }

    // ===================== Login and users =====================

    private String login(HttpExchange ex, Map<String, String> b) {
        try {
            // POLYMORPHISM: the variable is a User, the object is an Admin or a Reporter
            User user = logins.authenticate(get(b, "username"), get(b, "password"));
            byte[] bytes = new byte[24];
            random.nextBytes(bytes);
            StringBuilder token = new StringBuilder();
            for (byte x : bytes) {
                token.append(String.format("%02x", x));
            }
            sessions.put(token.toString(), user);
            ex.getResponseHeaders().add("Set-Cookie", "session=" + token + "; Path=/; HttpOnly; SameSite=Lax");
            return Json.obj("ok", true, "role", user.displayRole());
        } catch (IllegalArgumentException e) {
            throw new HttpError(401, e.getMessage());
        }
    }

    private String changePassword(User user, Map<String, String> b) {
        if (!user.login(user.getUsername(), get(b, "current"))) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        ValidationUtil.validatePassword(get(b, "newPassword"));
        user.setPassword(get(b, "newPassword"));
        return Json.obj("ok", true);
    }

    private String listUsers() {
        List<String> items = new ArrayList<>();
        for (User u : logins.getUsers()) {
            items.add(Json.obj("name", u.getName(), "username", u.getUsername(), "role", u.displayRole()));
        }
        return Json.array(items);
    }

    private String token(HttpExchange ex) {
        String cookies = ex.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) {
            for (String part : cookies.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals("session")) {
                    return kv[1];
                }
            }
        }
        return "";
    }

    private User currentUser(HttpExchange ex) {
        return sessions.get(token(ex));
    }

    private User requireUser(HttpExchange ex) {
        User user = currentUser(ex);
        if (user == null) {
            throw new HttpError(401, "Please log in.");
        }
        return user;
    }

    private Admin requireAdmin(User user) {
        if (user instanceof Admin) {
            return (Admin) user;
        }
        throw new HttpError(403, "Only the Barangay Administrator can do this.");
    }

    // ===================== Reports =====================

    private DamageReport findOwned(User user, String id) {
        DamageReport report = manager.findById(id);
        if (report == null) {
            throw new HttpError(404, "Report not found.");
        }
        if (!user.canManageReports() && !report.getReporterName().equalsIgnoreCase(user.getName())) {
            throw new HttpError(403, "You can only open your own reports.");
        }
        return report;
    }

    private String listReports(User user, Map<String, String> q) {
        ArrayList<DamageReport> base = user.canManageReports()
                ? manager.getAllReports() : manager.getReportsByReporter(user.getName());

        String keyword = q.getOrDefault("q", "").trim();
        if (!keyword.isEmpty()) {
            ArrayList<DamageReport> found = manager.search(q.getOrDefault("criteria", "All Fields"), keyword);
            ArrayList<DamageReport> both = new ArrayList<>();
            for (DamageReport r : base) {
                for (DamageReport f : found) {
                    if (f.getReportId().equals(r.getReportId())) {
                        both.add(r);
                        break;
                    }
                }
            }
            base = both;
        }
        base = manager.filterBySeverity(base, q.getOrDefault("severity", "All"));
        base = manager.filterByStatus(base, q.getOrDefault("status", "All Status"));

        int size = Math.max(1, Math.min(50, number(q.get("size"), 5)));
        int total = base.size();
        int pages = Math.max(1, (total + size - 1) / size);
        int page = Math.max(1, Math.min(pages, number(q.get("page"), 1)));

        List<String> items = new ArrayList<>();
        for (int i = (page - 1) * size; i < Math.min(total, page * size); i++) {
            items.add(reportJson(base.get(i)));
        }
        return Json.obj("total", total, "page", page, "pages", pages, "size", size, "items", Json.raw(Json.array(items)));
    }

    private String createReport(User user, Map<String, String> b) throws IOException {
        DamageReport report = buildReport(user, b, null);
        // The role decides which method is used (inheritance + polymorphism)
        if (user instanceof Reporter) {
            ((Reporter) user).submitReport(manager, report);
        } else {
            manager.addReport(report);
        }
        return Json.obj("id", report.getReportId(), "priority", report.getPriority(), "severity", report.getSeverity());
    }

    private String updateReport(Admin admin, DamageReport existing, Map<String, String> b) throws IOException {
        DamageReport report = buildReport(admin, b, existing);
        admin.updateReport(manager, report);
        return Json.obj("ok", true, "id", report.getReportId());
    }

    /** Checks every field (ValidationUtil) and builds the DamageReport object. */
    private DamageReport buildReport(User user, Map<String, String> b, DamageReport existing) throws IOException {
        String name = user.canManageReports() ? get(b, "reporterName") : user.getName();
        ValidationUtil.requireText(name, "Reporter Name");
        ValidationUtil.validateContactNumber(get(b, "contact"));
        ValidationUtil.requireText(get(b, "barangay"), "Barangay");
        ValidationUtil.requireText(get(b, "facilityName"), "Facility Name");
        ValidationUtil.validateFacilityType(get(b, "facilityType"));
        ValidationUtil.validateDamageType(get(b, "damageType"));
        ValidationUtil.validateSeverity(get(b, "severity"));
        double cost = ValidationUtil.parseDamageCost(get(b, "cost"));
        ValidationUtil.validateDescription(get(b, "description"));

        boolean isNew = (existing == null);
        String id = isNew ? manager.generateNextId() : existing.getReportId();
        String photoPath = isNew ? "" : existing.getPhotoPath();
        if (!get(b, "photoData").isEmpty()) {
            String saved = savePhoto(id, get(b, "photoName"), get(b, "photoData"));
            if (!photoPath.isEmpty() && !photoPath.equals(saved)) {
                deletePhotoFile(photoPath);
            }
            photoPath = saved;
        } else if (get(b, "removePhoto").equals("true")) {
            deletePhotoFile(photoPath);
            photoPath = "";
        }

        Facility facility = new Facility(get(b, "facilityName").trim(), get(b, "facilityType"), get(b, "barangay").trim());
        return new DamageReport(id, name.trim(), get(b, "contact").trim(), facility, get(b, "damageType"),
                get(b, "description").trim(), get(b, "severity"), cost,
                isNew ? LocalDate.now().toString() : existing.getDateReported(),
                isNew ? "REPORTED" : existing.getStatus(), photoPath);
    }

    private String savePhoto(String id, String fileName, String dataUrl) throws IOException {
        String base64 = dataUrl.contains(",") ? dataUrl.substring(dataUrl.indexOf(',') + 1) : dataUrl;
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The photo data is not valid.");
        }
        String extension = ValidationUtil.validatePhotoUpload(fileName, bytes);
        if (!photosDir.exists() && !photosDir.mkdirs()) {
            throw new IllegalStateException("Could not create the photos folder.");
        }
        Files.write(new File(photosDir, id + extension).toPath(), bytes);
        return "photos/" + id + extension;
    }

    private void deletePhotoFile(String photoPath) {
        if (photoPath != null && photoPath.startsWith("photos/")) {
            File file = new File(photosDir, photoPath.substring("photos/".length()));
            if (file.exists() && !file.delete()) {
                System.err.println("Could not delete photo " + file);
            }
        }
    }

    private String reportJson(DamageReport r) {
        return Json.obj("id", r.getReportId(), "reporter", r.getReporterName(), "contact", r.getContactNumber(),
                "barangay", r.getBarangay(), "facilityName", r.getFacilityName(), "facilityType", r.getFacilityType(),
                "damageType", r.getDamageType(), "description", r.getDescription(), "severity", r.getSeverity(),
                "priority", r.getPriority(), "cost", r.getEstimatedCost(), "date", r.getDateReported(),
                "status", r.getStatus(), "photo", r.hasPhoto() ? "/" + r.getPhotoPath() : "");
    }

    // ===================== Dashboard numbers =====================

    private String stats(User user) {
        if (!user.canManageReports()) {
            ArrayList<DamageReport> mine = manager.getReportsByReporter(user.getName());
            int repaired = 0;
            int review = 0;
            int high = 0;
            for (DamageReport r : mine) {
                if (r.getStatus().equals("REPAIRED")) {
                    repaired++;
                }
                if (r.getStatus().equals("UNDER REVIEW")) {
                    review++;
                }
                if (r.getSeverity().equals("HIGH")) {
                    high++;
                }
            }
            return Json.obj("manage", false, "total", mine.size(), "repaired", repaired, "review", review,
                    "high", high, "activity", Json.raw(activityJson(manager.getRecentActivities(3, user.getName()))));
        }

        List<String> facility = new ArrayList<>();
        int grouped = 0;
        for (int i = 0; i < FACILITY_GROUPS.length; i++) {
            int count = manager.countByFacilityType(FACILITY_GROUPS[i]);
            grouped += count;
            facility.add(Json.obj("label", FACILITY_LABELS[i], "count", count));
        }
        facility.add(Json.obj("label", FACILITY_LABELS[FACILITY_GROUPS.length], "count", manager.getTotalReports() - grouped));

        return Json.obj("manage", true,
                "total", manager.getTotalReports(), "totalNew", manager.countNewThisWeek(null, null),
                "critical", manager.countBySeverity("CRITICAL"), "criticalNew", manager.countNewThisWeek("CRITICAL", null),
                "high", manager.countBySeverity("HIGH"), "highNew", manager.countNewThisWeek("HIGH", null),
                "review", manager.countByStatus("UNDER REVIEW"), "reviewNew", manager.countNewThisWeek(null, "UNDER REVIEW"),
                "progress", manager.countByStatus("IN PROGRESS"), "progressNew", manager.countNewThisWeek(null, "IN PROGRESS"),
                "repaired", manager.countByStatus("REPAIRED"), "repairedNew", manager.countNewThisWeek(null, "REPAIRED"),
                "pending", manager.countByStatus("REPORTED"),
                "severity", Json.raw("[" + manager.countBySeverity("LOW") + "," + manager.countBySeverity("MODERATE")
                        + "," + manager.countBySeverity("HIGH") + "," + manager.countBySeverity("CRITICAL") + "]"),
                "facility", Json.raw(Json.array(facility)),
                "activity", Json.raw(activityJson(manager.getRecentActivities(5, null))),
                "cost", manager.getTotalEstimatedCost(), "lastSaved", manager.getLastSavedText(),
                "storage", manager.getStorageDescription(), "version", "v1.0.0");
    }

    private String activityJson(ArrayList<ActivityEntry> entries) {
        List<String> items = new ArrayList<>();
        for (ActivityEntry e : entries) {
            items.add(Json.obj("kind", e.getKind(), "title", e.getTitle(), "detail", e.getDetail(), "time", e.getTime()));
        }
        return Json.array(items);
    }

    // ===================== Photos and web page files =====================

    private void handlePhoto(HttpExchange ex) throws IOException {
        if (currentUser(ex) == null) {
            send(ex, 401, "text/plain", "Please log in.".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String name = ex.getRequestURI().getPath().substring("/photos/".length());
        File file = new File(photosDir, name);
        if (!name.matches("[A-Za-z0-9._-]+") || !file.isFile()) {
            send(ex, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(ex, 200, name.endsWith(".png") ? "image/png" : "image/jpeg", Files.readAllBytes(file.toPath()));
    }

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        File file = new File(webDir, path.substring(1));
        String webRoot = webDir.getCanonicalPath() + File.separator;
        if (!file.getCanonicalPath().startsWith(webRoot) || !file.isFile()) {   // blocks ../ tricks
            send(ex, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        String name = file.getName().toLowerCase();
        String type = "application/octet-stream";
        if (name.endsWith(".html")) {
            type = "text/html; charset=utf-8";
        } else if (name.endsWith(".css")) {
            type = "text/css; charset=utf-8";
        } else if (name.endsWith(".js")) {
            type = "application/javascript; charset=utf-8";
        } else if (name.endsWith(".png")) {
            type = "image/png";
        } else if (name.endsWith(".svg")) {
            type = "image/svg+xml";
        }
        ex.getResponseHeaders().add("Cache-Control", "no-cache");
        send(ex, 200, type, Files.readAllBytes(file.toPath()));
    }

    // ===================== Small helpers =====================

    private Map<String, String> body(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                if (out.size() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException("The request is too large.");
                }
            }
            String text = out.toString("UTF-8");
            return text.trim().isEmpty() ? new HashMap<>() : Json.parse(text);
        }
    }

    private static String get(Map<String, String> map, String key) {
        String value = map.get(key);
        return value == null ? "" : value;
    }

    private static int number(String text, int fallback) {
        try {
            return text == null ? fallback : Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static Map<String, String> query(URI uri) throws IOException {
        Map<String, String> map = new HashMap<>();
        String raw = uri.getRawQuery();
        if (raw != null) {
            for (String pair : raw.split("&")) {
                String[] kv = pair.split("=", 2);
                map.put(URLDecoder.decode(kv[0], "UTF-8"), kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "");
            }
        }
        return map;
    }

    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        send(ex, status, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private void send(HttpExchange ex, int status, String type, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, data.length);
        ex.getResponseBody().write(data);
        ex.close();
    }
}
