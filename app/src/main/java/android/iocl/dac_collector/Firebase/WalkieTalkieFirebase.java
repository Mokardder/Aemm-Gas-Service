package android.iocl.dac_collector.Firebase;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.FirebaseDatabase;

public final class WalkieTalkieFirebase {

    private static final String APP_NAME = "walkie-talkie";
    private static final String DATABASE_URL =
            "https://aemmgasservice-default-rtdb.firebaseio.com/";
    private static final String API_KEY = "AIzaSyBYWq6irELsdHwSUHAZksBbMaS5_yQQrBw";
    private static final String APPLICATION_ID =
            "1:941268635139:android:ea5a41518325af7c634836";
    private static final String PROJECT_ID = "aemmgasservice";

    private static FirebaseDatabase database;

    private WalkieTalkieFirebase() {
    }

    public static synchronized FirebaseDatabase getDatabase(Context context) {
        if (database == null) {
            Context applicationContext = context.getApplicationContext();
            FirebaseApp walkieTalkieApp = null;
            for (FirebaseApp app : FirebaseApp.getApps(applicationContext)) {
                if (APP_NAME.equals(app.getName())) {
                    walkieTalkieApp = app;
                    break;
                }
            }

            if (walkieTalkieApp == null) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApiKey(API_KEY)
                        .setApplicationId(APPLICATION_ID)
                        .setProjectId(PROJECT_ID)
                        .setDatabaseUrl(DATABASE_URL)
                        .build();
                walkieTalkieApp = FirebaseApp.initializeApp(
                        applicationContext,
                        options,
                        APP_NAME
                );
            }

            database = FirebaseDatabase.getInstance(walkieTalkieApp, DATABASE_URL);
        }
        return database;
    }
}
