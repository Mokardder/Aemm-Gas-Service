package android.iocl.dac_collector.SyncAdapters;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class AuthenticatorService extends Service {
    private AppAuthenticator authenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        authenticator = new AppAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return authenticator.getIBinder();
    }
}