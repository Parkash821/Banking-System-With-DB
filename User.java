// --- 2. model/User.java ---
package model;

import java.util.Objects;
import java.util.UUID;

// The User class represents a bank user
public class User {
    private String id;
    private String username;
    private String passwordHash; // Hashed password for security
    private String fullName;
    private boolean isAdmin;

    // Constructor
    public User(String id, String username, String passwordHash, String fullName, boolean isAdmin) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.isAdmin = isAdmin;
    }

    // Constructor for creating a new user (ID is generated internally)
    public User(String username, String passwordHash, String fullName, boolean isAdmin) {
        this(UUID.randomUUID().toString(), username, passwordHash, fullName, isAdmin);
    }

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isAdmin() { return isAdmin; }
    public String getFullName() { return fullName; }

    // Setters (for updates)
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
               "id='" + id + '\'' +
               ", username='" + username + '\'' +
               ", fullName='" + fullName + '\'' +
               ", isAdmin=" + isAdmin +
               '}';
    }
}