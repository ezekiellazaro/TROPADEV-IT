package barangaydamagereporter.model;

/**
 * OOP CONCEPT: ABSTRACTION + INHERITANCE (parent class)
 * User is an ABSTRACT class: it cannot be created with "new User(...)".
 * It holds what every user has in common. Reporter and Admin extend it.
 */
public abstract class User {

    // OOP CONCEPT: ENCAPSULATION - all fields are private
    private String username;
    private String password;
    private String name;

    public User(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }

    // ---- Getters and setters (controlled access to the private fields) ----
    public String getUsername() { return username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // There is intentionally NO getPassword(): the password can be checked
    // with login() or changed with setPassword(), but never read directly.
    public void setPassword(String password) { this.password = password; }

    /** Checks the entered credentials against this user's credentials. */
    public boolean login(String enteredUsername, String enteredPassword) {
        return username.equals(enteredUsername) && password.equals(enteredPassword);
    }

    // OOP CONCEPT: ABSTRACTION - abstract methods have NO body here.
    // Every child class MUST provide its own version.
    public abstract String displayRole();
    public abstract boolean canManageReports();
    public abstract String getDashboardTitle();
    public abstract String getDashboardSubtitle();

    /** Normal method that child classes may OVERRIDE (polymorphism). */
    public String getWelcomeMessage() {
        return "Welcome, " + name + "!";
    }
}
