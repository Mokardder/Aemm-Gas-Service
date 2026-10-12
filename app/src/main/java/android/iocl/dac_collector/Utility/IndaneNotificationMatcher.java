package android.iocl.dac_collector.Utility;

import java.util.Locale;

public final class IndaneNotificationMatcher {

    private IndaneNotificationMatcher() {
    }

    public static boolean shouldDismissNotification(
            String title,
            String text,
            String lastIndaneSmsBody,
            String lastIndaneSender
    ) {
        String normalizedTitle = normalize(title);
        String normalizedText = normalize(text);
        String notificationText = normalizedTitle + " " + normalizedText;

        if (notificationText.contains("sensitive notification content hidden")) {
            return true;
        }

        if (lastIndaneSmsBody == null || lastIndaneSmsBody.trim().isEmpty()) {
            return false;
        }

        String normalizedSmsBody = normalize(lastIndaneSmsBody);

        if (normalizedSmsBody.isEmpty()) {
            return false;
        }

        boolean indaneInSms = containsIndaneKeyword(normalizedSmsBody);
        if (!indaneInSms) {
            return false;
        }

        boolean hasDeliveredText = notificationText.contains("delivered");
        boolean titleOrTextContainsIndane = containsIndaneKeyword(notificationText);

        boolean smsBodyMatchesNotification =
                notificationText.contains(normalizedSmsBody)
                        || normalizedSmsBody.contains(normalizedTitle)
                        || normalizedSmsBody.contains(normalizedText);

        if (lastIndaneSender != null && !lastIndaneSender.trim().isEmpty()) {
            String normalizedSender = normalize(lastIndaneSender);
            smsBodyMatchesNotification = smsBodyMatchesNotification
                    || notificationText.contains(normalizedSender);
        }

        return smsBodyMatchesNotification
            || (titleOrTextContainsIndane && hasDeliveredText);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean containsIndaneKeyword(String value) {
        if (value == null) {
            return false;
        }

        return value.contains("indane")
                || value.contains("indianoil")
                || value.contains("indian oil");
    }
}