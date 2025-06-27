// --- 6. util/IdGenerator.java ---
package util;

import java.util.UUID;

// IdGenerator is a helper class for generating unique IDs
public class IdGenerator {
    public static String generateUniqueId() {
        return UUID.randomUUID().toString();
    }
}
