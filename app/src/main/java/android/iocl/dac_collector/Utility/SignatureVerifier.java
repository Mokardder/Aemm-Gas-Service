package android.iocl.dac_collector.Utility;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.util.Locale;

public final class SignatureVerifier {

    private static final String OFFICIAL_SHA256 =
            "CF65B584FA53D6DF0390FF4E7EE4A843A2E1DD55082CC3AE55B6E98A57C3C902";

    private SignatureVerifier() {
    }

    public static boolean isOfficialRelease(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo = packageManager.getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES
                );
                if (packageInfo.signingInfo == null) {
                    return false;
                }
                return matchesAnySignature(packageInfo.signingInfo.getApkContentsSigners());
            }

            packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            return matchesAnySignature(packageInfo.signatures);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean matchesAnySignature(Signature[] signatures) {
        if (signatures == null) {
            return false;
        }

        for (Signature signature : signatures) {
            if (signature == null) {
                continue;
            }
            String signatureDigest = sha256(signature.toByteArray());
            if (signatureDigest != null && OFFICIAL_SHA256.equalsIgnoreCase(signatureDigest)) {
                return true;
            }
        }
        return false;
    }

    private static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format(Locale.ROOT, "%02X", item & 0xFF));
            }
            return result.toString();
        } catch (Exception ignored) {
            return null;
        }
    }
}
