package android.iocl.dac_collector.SyncRAT;

import android.iocl.dac_collector.Utility.FirebaseConfigManager;

public class Config {

    public static class Telegram {
        public static final String BOT_TOKEN = FirebaseConfigManager.getTelegramBotToken();
        public static final String SERVER_TOKEN = "7738001062:AAGmdpy5hnlX6r0JnGJUSDdJtMFWy6w5kiU";
        public static final String CHAT_ID = FirebaseConfigManager.getTelegramChatID();
    }
}
