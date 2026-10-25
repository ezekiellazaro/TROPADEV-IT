package barangaydamagereporter.util;

import java.io.File;

/**
 * INPUT VALIDATION in one place. Every method either passes quietly or throws
 * an IllegalArgumentException with a user-friendly message that the GUI shows.
 */
public class ValidationUtil {

    // COLLECTIONS: arrays holding the allowed choices (also used by the dropdowns)
    public static final String[] FACILITY_TYPES = {
        "Barangay Hall", "Health Center", "Day Care Center", "Public School",
        "Barangay Road", "Bridge", "Drainage System", "Street Light",
        "Waiting Shed", "Basketball Court", "Public Comfort Room", "Other"
    };
    public static final String[] DAMAGE_TYPES = {
        "Flood Damage", "Structural Damage", "Electrical Damage", "Fire Damage",
        "Road Damage", "Water Damage", "Broken Equipment", "Other"
    };
    public static final String[] SEVERITIES = {"LOW", "MODERATE", "HIGH", "CRITICAL"};
    // The ORDER of this array is the allowed status workflow.
    public static final String[] STATUSES = {
        "REPORTED", "UNDER REVIEW", "IN PROGRESS", "REPAIRED", "CLOSED"
    };

    public static final double MAX_COST = 1_000_000_000.0;

    private ValidationUtil() { } // utility class: no objects needed

    /** Required field: must not be null or blank. */
    public static void requireText(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be empty.");
        }
    }

    /** Contact number: digits only (optional leading +), 7 to 13 digits. */
    public static void validateContactNumber(String contact) {
        requireText(contact, "Contact Number");
        String cleaned = contact.trim().replaceAll("[\\s-]", "");
        if (!cleaned.matches("\\+?\\d{7,13}")) {
            throw new IllegalArgumentException(
                "Contact Number should contain valid numbers only (example: 09171234567).");
        }
    }

    /**
     * Damage cost: numbers only, not empty, not negative.
     * EXCEPTION HANDLING: catches NumberFormatException from Double.parseDouble.
     */
    public static double parseDamageCost(String text) {
        requireText(text, "Estimated Cost");
        String cleaned = text.trim().replace(",", "");
        if (!cleaned.matches("-?\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException(
                "Please enter a valid damage cost (example: 15000 or 2500.50).");
        }
        double cost;
        try {
            cost = Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Please enter a valid damage cost.");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("Estimated Cost cannot be negative.");
        }
        if (cost > MAX_COST) {
            throw new IllegalArgumentException("Estimated Cost is too large.");
        }
        return cost;
    }

    /** The value must be one of the allowed choices (loops through the array). */
    public static void validateInList(String value, String[] allowed, String label) {
        if (value != null) {
            for (String item : allowed) {
                if (item.equals(value)) {
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Please select a valid " + label + ".");
    }

    public static void validateSeverity(String severity) {
        validateInList(severity, SEVERITIES, "Severity");
    }

    public static void validateFacilityType(String type) {
        validateInList(type, FACILITY_TYPES, "Facility Type");
    }

    public static void validateDamageType(String type) {
        validateInList(type, DAMAGE_TYPES, "Damage Type");
    }

    /** Description must contain enough information. */
    public static void validateDescription(String description) {
        requireText(description, "Description");
        if (description.trim().length() < 10) {
            throw new IllegalArgumentException(
                "Description must contain enough information (at least 10 characters).");
        }
        if (description.trim().length() > 500) {
            throw new IllegalArgumentException("Description is too long (maximum 500 characters).");
        }
    }

    public static void validateUsername(String username) {
        requireText(username, "Username");
        if (username.trim().contains(" ")) {
            throw new IllegalArgumentException("Username must not contain spaces.");
        }
    }

    public static void validatePassword(String password) {
        requireText(password, "Password");
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }
    }

    /** Optional photo: must exist, be JPG/JPEG/PNG and at most 5 MB. */
    public static void validatePhoto(File file) {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("The selected photo could not be found.");
        }
        String name = file.getName().toLowerCase();
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))) {
            throw new IllegalArgumentException("Photo must be a JPG, JPEG or PNG file.");
        }
        if (file.length() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("Photo is too large (maximum size is 5 MB).");
        }
    }

    /** Photo sent from the website: JPG or PNG, real image bytes, at most 5 MB. Returns ".png" or ".jpg". */
    public static String validatePhotoUpload(String fileName, byte[] data) {
        String name = (fileName == null) ? "" : fileName.toLowerCase();
        String extension;
        if (name.endsWith(".png")) {
            extension = ".png";
        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            extension = ".jpg";
        } else {
            throw new IllegalArgumentException("Photo must be a JPG, JPEG or PNG file.");
        }
        if (data == null || data.length < 8) {
            throw new IllegalArgumentException("The selected photo is empty.");
        }
        if (data.length > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Photo is too large (maximum size is 5 MB).");
        }
        boolean isPng = (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
        boolean isJpg = (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
        if (!(isPng || isJpg)) {
            throw new IllegalArgumentException("The file is not a real JPG or PNG image.");
        }
        return extension;
    }
}
