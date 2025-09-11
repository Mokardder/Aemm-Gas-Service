package android.iocl.dac_collector.Utility;

import android.util.Base64;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ObfuscatedEncryptor
 *
 * A simple deterministic "encryption" utility for Android.
 * This class allows encoding and decoding of strings using:
 * - Base64 encoding
 * - A time-based deterministic key derived from the current date and time.
 *
 * The class is intentionally obfuscated to make reverse-engineering harder:
 * - Method names are non-descriptive (x1, y1, z2, z3)
 * - The key is embedded in the encoded string
 * - Only one SharedPreferences entry is needed if storing values
 *
 * IMPORTANT NOTES:
 * 1. This is NOT cryptographically secure. It only prevents casual inspection.
 * 2. The "key" is derived from the current date + hour + minute (yyyyMMddHHmm).
 *    - This means the encoded string can only be decrypted correctly within the same minute.
 *    - For longer validity, you must modify the key derivation.
 * 3. Uses android.util.Base64 to maintain compatibility with all Android API levels.
 */
public class ObfuscatedEncryptor {

    /**
     * Encrypt a plain text string.
     * Combines the input string with a deterministic key and encodes it in Base64.
     *
     * @param s The plain text string to encrypt
     * @return Base64 encoded string containing the text and key
     */
    public static String x1(String s) {
        String k = z2();               // generate the deterministic key
        String c = s + ":" + k;        // combine plain text and key
        return Base64.encodeToString(c.getBytes(), Base64.NO_WRAP); // encode as Base64
    }

    /**
     * Decrypt a previously encrypted string.
     * Validates the embedded key against the deterministic key for the current minute.
     *
     * @param e Base64 encoded string containing text + key
     * @return The original plain text if key matches, null otherwise
     */
    public static String y1(String e) {
        try {
            String d = new String(Base64.decode(e, Base64.NO_WRAP)); // decode Base64
            String[] p = d.split(":", 2);                            // split text and key
            if (p.length < 2) return null;                           // invalid format
            String t = p[0];                                         // original text
            String kActual = p[1];                                   // key stored in encoded string
            String kExpected = z2();                                 // generate current deterministic key
            if (kActual.equals(kExpected)) return t;                 // return text if key matches
        } catch (Exception ignored) {
            // catch Base64 decode errors or unexpected splits
        }
        return null; // decryption failed
    }

    /**
     * Generate deterministic key for encryption/decryption.
     * Encodes the current time string (yyyyMMddHHmm) in Base64.
     *
     * @return Base64 encoded deterministic key string
     */
    private static String z2() {
        String raw = z3();                        // get current date+time string
        return Base64.encodeToString(raw.getBytes(), Base64.NO_WRAP);
    }

    /**
     * Get current time string used as base for deterministic key.
     * Format: yyyyMMddHHmm (year, month, day, hour, minute)
     *
     * @return Time string
     */
    private static String z3() {
        return new SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
                .format(new Date());
    }
}
