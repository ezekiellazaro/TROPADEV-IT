package barangaydamagereporter.service;

import barangaydamagereporter.model.Admin;
import barangaydamagereporter.model.Reporter;
import barangaydamagereporter.model.User;
import barangaydamagereporter.util.ValidationUtil;
import java.util.ArrayList;

/**
 * Checks usernames and passwords and keeps the list of accounts.
 * OOP CONCEPT: POLYMORPHISM - one ArrayList<User> holds BOTH Admin and Reporter
 * objects because each of them "is a" User.
 */
public class LoginManager {

    private final ArrayList<User> users = new ArrayList<>();

    public LoginManager() {
        users.add(new Admin("admin", "admin123", "Barangay Admin"));
        users.add(new Reporter("resident", "resident123", "Juan Dela Cruz"));
    }

    /** Returns the matching user, or throws IllegalArgumentException with a clear message. */
    public User authenticate(String username, String password) {
        ValidationUtil.validateUsername(username);
        ValidationUtil.requireText(password, "Password");

        String name = username.trim();
        for (User user : users) {
            if (user.getUsername().equals(name)) {
                if (user.login(name, password)) {
                    return user; // could be an Admin or a Reporter
                }
                throw new IllegalArgumentException("Incorrect password. Please try again.");
            }
        }
        throw new IllegalArgumentException("Username not found. Please check your username.");
    }

    /** A copy of the account list (for the User Management screen). */
    public ArrayList<User> getUsers() {
        return new ArrayList<>(users);
    }

    /** Creates a new Reporter account (accounts are kept in memory only). */
    public void addReporter(String fullName, String username, String password) {
        ValidationUtil.requireText(fullName, "Full Name");
        ValidationUtil.validateUsername(username);
        ValidationUtil.validatePassword(password);
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username.trim())) {
                throw new IllegalArgumentException("Username \"" + username.trim() + "\" is already taken.");
            }
        }
        users.add(new Reporter(username.trim(), password, fullName.trim()));
    }
}
