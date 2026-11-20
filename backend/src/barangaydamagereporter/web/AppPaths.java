package barangaydamagereporter.web;

import java.io.File;

/**
 * Finds the project folder (the one that contains frontend/index.html) so the program works
 * no matter where it is started from (run.bat, VS Code "Run", IDE, another folder...).
 */
public final class AppPaths {

    private static final File BASE = findBase();

    private AppPaths() { }

    public static File base() { return BASE; }

    public static File file(String name) { return new File(BASE, name); }

    /** Folder for the database file, the text-file backup and uploaded photos (created if missing). */
    public static File database() {
        File d = new File(BASE, "database");
        d.mkdirs();
        return d;
    }

    private static boolean isProject(File d) {
        return d != null && new File(d, "frontend/index.html").isFile();
    }

    private static File findBase() {
        File cwd = new File(System.getProperty("user.dir")).getAbsoluteFile();
        if (isProject(cwd)) {
            return cwd;
        }
        File[] kids = cwd.listFiles(File::isDirectory);        // project is inside the current folder
        if (kids != null) {
            for (File k : kids) {
                if (isProject(k)) {
                    return k;
                }
                File[] deeper = k.listFiles(File::isDirectory);
                if (deeper != null) {
                    for (File g : deeper) {
                        if (isProject(g)) {
                            return g;
                        }
                    }
                }
            }
        }
        for (File d = cwd.getParentFile(); d != null; d = d.getParentFile()) {   // project is above
            if (isProject(d)) {
                return d;
            }
        }
        try {                                                   // next to the compiled classes
            File loc = new File(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            for (File d = loc; d != null; d = d.getParentFile()) {
                if (isProject(d)) {
                    return d;
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return cwd;
    }
}
