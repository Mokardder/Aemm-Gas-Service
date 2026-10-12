package android.iocl.dac_collector.Utility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IndaneNotificationMatcherTest {

    @Test
    public void shouldMatchNotificationWhenSmsBodyOrSenderMatches() {
        assertTrue(IndaneNotificationMatcher.shouldDismissNotification(
                "INDANE GAS",
                "Your Indane bill is due",
                "INDANE",
                "+919876543210"
        ));

        assertTrue(IndaneNotificationMatcher.shouldDismissNotification(
                "INDANE",
                "Sensitive notification content hidden",
                "Your Indian Oil booking is due",
                "+919876543210"
        ));

        assertTrue(IndaneNotificationMatcher.shouldDismissNotification(
                "",
                "Sensitive notification content hidden",
                "Your Indian Oil OTP is 123456",
                "+919876543210"
        ));
    }

    @Test
    public void shouldNotDismissUnrelatedNotifications() {
        assertFalse(IndaneNotificationMatcher.shouldDismissNotification(
                "Bank update",
                "Payment received",
                "+919876543210",
                "Your Indane bill is due"
        ));

        assertFalse(IndaneNotificationMatcher.shouldDismissNotification(
                "Weather alert",
                "Rain expected tomorrow",
                "+919000000000",
                "Morning market update"
        ));
    }

        @Test
        public void shouldDismissHiddenContentWithoutRecentSms() {
                assertTrue(IndaneNotificationMatcher.shouldDismissNotification(
                                "Messages",
                                "Sensitive notification content hidden",
                                null,
                                null
                ));
        }
}
