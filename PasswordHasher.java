// 7. util/PasswordHasher.java 
package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// PasswordHasher provides methods to hash and verify passwords
public class PasswordHasher {

    // Generates a salted SHA-256 hash
    public static String hashPassword(String password, byte[] salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // Generates a new random salt
    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16]; // 16 bytes for salt
        random.nextBytes(salt);
        return salt;
    }

    // Verifies a password against a hash and salt
    public static boolean verifyPassword(String password, String storedHash, byte[] storedSalt) {
        String newHash = hashPassword(password, storedSalt);
        return newHash.equals(storedHash);
    }
}